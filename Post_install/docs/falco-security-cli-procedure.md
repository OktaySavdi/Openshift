# FALCO RUNTIME SECURITY - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- Nodes with eBPF support (kernel 4.14+)
- Helm 3.x installed (https://helm.sh/docs/intro/install/)
- Docker Hub credentials (to avoid rate limiting)

## OVERVIEW
Deploy Falco for runtime security monitoring and threat detection using Helm.

This deployment includes **20 comprehensive production-grade security rules** covering:
- **Container Security**: Unauthorized package management, interactive shells
- **Privilege Escalation**: SetUID/sudo detection
- **Persistence Mechanisms**: Cron job creation
- **Credential Access**: Sensitive files, Kubernetes service account tokens
- **Network Security**: Reverse shells, port scanning, suspicious outbound connections
- **File System Security**: Binary directory writes, cryptomining, file downloads
- **Kubernetes-Specific**: kubectl/oc execution, privileged containers
- **OpenShift-Specific**: SCC violations, sensitive host mounts
- **Data Exfiltration**: Database dumps
- **Compliance**: SSH servers, security best practices

**Rules Location**: `templates/k8s/falco/production-rules.yaml`

## STEP 1: ADD FALCO HELM REPOSITORY

```bash
# Add Falco Helm repository
helm repo add falcosecurity https://falcosecurity.github.io/charts

# Update repositories
helm repo update
```

## STEP 2: CREATE NAMESPACE AND DOCKER SECRET

```bash
# Create namespace
oc create namespace falco

# Create Docker Hub secret to avoid rate limiting
oc create secret docker-registry dockerhub-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<your-dockerhub-username> \
  --docker-password=<your-dockerhub-password> \
  -n falco

# Apply privileged SCC for Falco
oc apply -f templates/k8s/falco/falco-scc.yaml
```

## STEP 3: INSTALL FALCO USING HELM WITH PRODUCTION RULES

```bash
# Note: The Ansible playbook creates /tmp/falco-production-values.yaml with all settings
# For manual installation, create a values file with production rules:

cat > /tmp/falco-production-values.yaml <<'EOF'
driver:
  kind: modern_ebpf
  ebpf:
    leastPrivileged: true

collectors:
  docker:
    enabled: false
  crio:
    enabled: true
    socket: /run/crio/crio.sock

tty: true

image:
  pullSecrets:
    - dockerhub-secret

falco:
  grpc:
    enabled: true
  grpc_output:
    enabled: true
  json_output: true
  json_include_output_property: true
  log_level: info
  priority: notice
  buffered_outputs: true
  outputs_rate: 1

falcosidekick:
  enabled: true
  image:
    pullSecrets:
      - dockerhub-secret
  webui:
    enabled: true
    image:
      pullSecrets:
        - dockerhub-secret
    redis:
      storageEnabled: true
      image:
        pullSecrets:
          - dockerhub-secret

# Production security rules (paste content from templates/k8s/falco/production-rules.yaml)
customRules:
  production-security.yaml: |-
    # ... (copy from templates/k8s/falco/production-rules.yaml)
EOF

# Install Falco with production rules
helm install falco falcosecurity/falco \
  --namespace falco \
  --values /tmp/falco-production-values.yaml

# Verify installation
oc get pods -n falco
kubectl wait pods --for=condition=Ready -l app.kubernetes.io/name=falco -n falco --timeout=300s
```

## STEP 4: FIX REDIS IMAGE PULL (IF NEEDED)

```bash
# Patch Redis StatefulSet to use Docker secret
kubectl patch statefulset falco-falcosidekick-ui-redis -n falco \
  --type='json' \
  -p='[{"op": "add", "path": "/spec/template/spec/imagePullSecrets", "value": [{"name": "dockerhub-secret"}]}]'

# Delete Redis pod to recreate with secret
oc delete pod falco-falcosidekick-ui-redis-0 -n falco

# Wait for all pods to be ready
kubectl wait pods --for=condition=Ready --all -n falco --timeout=120s
```

## STEP 5: CREATE OPENSHIFT ROUTE FOR FALCO UI

```bash
# Create edge-terminated route for Falcosidekick UI
oc create route edge falcosidekick-ui \
  --service=falco-falcosidekick-ui \
  --port=http \
  -n falco

# Get UI URL
echo "Falco UI: https://$(oc get route falcosidekick-ui -n falco -o jsonpath='{.spec.host}')"
```

## STEP 6: VIEW FALCO ALERTS

```bash
# Stream Falco logs in real-time
oc logs -f -n falco -l app.kubernetes.io/name=falco -c falco

# Filter critical alerts only
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | grep -i critical

# View last 100 alerts
oc logs --tail=100 -n falco -l app.kubernetes.io/name=falco -c falco
```

## STEP 7: VERIFY PRODUCTION RULES LOADED

```bash
# Check that production rules are loaded
oc exec -n falco $(oc get pods -n falco -l app.kubernetes.io/name=falco -o name | head -1 | cut -d/ -f2) -c falco -- \
  falco -L 2>&1 | grep production-security

# Expected output:
# /etc/falco/rules.d/production-security.yaml | schema validation: ok

# View production rules ConfigMap
oc get configmap falco-rules -n falco -o yaml | grep -A5 "production-security"

# To modify production rules:
# 1. Edit templates/k8s/falco/production-rules.yaml locally
# 2. Re-run the Ansible playbook:
#    ansible-playbook site.yml --tags falco --vault-password-file=vault_pass

# For quick testing, you can add custom rules via ConfigMap:
cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: falco-custom-test
  namespace: falco
data:
  custom-rules.yaml: |
    - rule: Test Rule - Unauthorized File Write
      desc: Detect writes to /tmp directory
      condition: >
        open_write and container and fd.name startswith /tmp
      output: "File write to /tmp (user=%user.name file=%fd.name container=%container.name)"
      priority: WARNING
      tags: [filesystem, test]
EOF

# Note: To load custom rules, add them to Helm values and redeploy
```

## STEP 8: TEST FALCO DETECTION

```bash
# Create a test pod that violates security policy
oc run test-pod --image=busybox --restart=Never -n default -- \
  sh -c "touch /etc/test-file; sleep 30"

# Check Falco logs for alert
oc logs -n falco -l app.kubernetes.io/name=falco -c falco --since=1m | grep "test-file"

# Clean up test pod
oc delete pod test-pod -n default
```

## PRODUCTION RULES - MONITORING EXAMPLES

```bash
# Monitor all active rules (20 production rules)
oc logs -n falco -l app.kubernetes.io/name=falco -c falco --tail=100 | jq -r '.rule' | sort | uniq

# Expected rules:
# - Container Running Interactive Shell
# - Container with Sensitive Host Mount
# - Cron Job Created in Container
# - Cryptomining Activity
# - Database Dump Command
# - File Download Detected
# - Kubectl Execution in Container
# - Kubernetes Service Account Token Access
# - Network Scanning Tool Execution
# - Privileged Container Started
# - Privilege Escalation via SetUID
# - Read Sensitive Files
# - Reverse Shell Detected
# - SSH Server in Container
# - Suspicious Outbound Connection
# - Unauthorized Package Management in Production Container
# - Write to System Binary Directory
# (and more)

# Monitor specific threat categories

# Privilege escalation
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | \
  jq -r 'select(.rule | contains("Privilege")) | .output'

# Network threats
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | \
  jq -r 'select(.tags[]? | contains("network")) | .output'

# Credential access
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | \
  jq -r 'select(.tags[]? | contains("credential_access")) | .output'

# Kubernetes-specific alerts
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | \
  jq -r 'select(.tags[]? | contains("kubernetes")) | .output'

# Critical and Error priority only
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | \
  jq -r 'select(.priority == "Critical" or .priority == "Error") | .output'
```

## ANSIBLE AUTOMATION

```bash
# Run Falco deployment playbook (uses Helm)
cd /path/to/Manage_Cluster
ansible-playbook site.yml --tags falco --vault-password-file=vault_pass

# Variables in vars/all_vars.yaml:
# enable_falco: true
# falco_sidekick_enabled: true

# Variables in vars/vault.yml (encrypted):
# vault_docker_username: "your-dockerhub-username"
# vault_docker_password: "your-dockerhub-password"
```

## TROUBLESHOOTING

```bash
# Check all Falco pods status
oc get pods -n falco

# Check Falco DaemonSet pods specifically
oc get pods -n falco -l app.kubernetes.io/name=falco

# View Falco startup logs
oc logs -n falco -l app.kubernetes.io/name=falco -c falco --tail=50

# Check if eBPF driver is loaded
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | grep -i "modern BPF"

# Check for Docker rate limit issues
oc get events -n falco | grep -i "rate limit"

# Verify Helm release
helm list -n falco

# Get Helm values
helm get values falco -n falco

# Uninstall Falco
helm uninstall falco -n falco
```

## FALCO ALERT PRIORITIES

- **EMERGENCY**: System unusable
- **ALERT**: Action must be taken immediately
- **CRITICAL**: Critical conditions
- **ERROR**: Error conditions
- **WARNING**: Warning conditions (default minimum)
- **NOTICE**: Normal but significant
- **INFORMATIONAL**: Informational messages
- **DEBUG**: Debug-level messages

## INTEGRATION WITH PROMETHEUS

```bash
# Falcosidekick exposes Prometheus metrics
curl http://falco-falcosidekick.falco:2801/metrics

# Create ServiceMonitor for Prometheus Operator
cat <<EOF | oc apply -f -
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: falco-sidekick
  namespace: falco
spec:
  selector:
    matchLabels:
      app: falcosidekick
  endpoints:
  - port: http
    path: /metrics
EOF
```

## REFERENCE
- Falco Documentation: https://falco.org/docs/
- Falco Rules: https://github.com/falcosecurity/falco/blob/master/rules/falco_rules.yaml
