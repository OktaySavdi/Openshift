# OpenShift Post-Installation Configuration

Ansible automation for OpenShift 4.14+ cluster configuration including certificates, LDAP, networking, storage, RBAC, and custom operators.

## Quick Start

### Prerequisites

- **OpenShift** 4.14+ with cluster-admin access
- **Ansible** 2.15.0+ with `kubernetes.core` and `community.general` collections
- **Python** 3.9+ with `kubernetes` and `openshift` modules

### Installation

```bash
# Install dependencies
pip install -r requirements.txt

# Set KUBECONFIG
export KUBECONFIG=/path/to/kubeconfig
oc whoami  # Verify cluster access
```

### Configuration

1. **Edit `vars/all_vars.yaml`** - Main configuration file with all feature flags:
   
   **Required settings:**
   ```yaml
   cluster_name: "myocpcluster.domain.com"
   base_domain: "domain.com"
   ```
   
   **Feature toggles (enable/disable as needed):**
   ```yaml
   # Core Infrastructure
   enable_infra: false                    # Dedicated infrastructure nodes
   enable_machine_labeling: true          # Label baremetal nodes
   
   # Certificates & Security
   enable_certificates: true              # API/Ingress certificates via cert-manager
   enable_ldap: true                      # LDAP authentication
   enable_ldap_group_sync: false          # LDAP group synchronization
   disable_self_provisioning: true        # Disable project self-provisioning
   enable_ssh_keys: true                  # SSH authorized keys on nodes
   
   # Networking
   enable_egress_ip: true                 # OVN-Kubernetes egress IPs
   enable_cluster_proxy: false            # Cluster-wide HTTP/HTTPS proxy
   
   # Storage & Registry
   enable_registry_storage: false         # Image registry NFS/PVC storage
   enable_image_pruning: true             # Automated image pruning
   
   # Operations
   enable_chrony: true                    # NTP time synchronization
   enable_dev_portal: true                # Developer console perspective
   enable_upgrade_optimization: true      # Cluster upgrade settings
   enable_log_forwarding: true            # External log forwarding
   enable_haproxy_logging: false          # HAProxy access logs
   enable_kdump: false                    # Kernel crash dump collection
   enable_opencost: true                  # Kubecost cost analysis and optimization
   enable_gt_operators: true              # Custom monitoring operators
   
   # Security & Compliance
   enable_compliance_operator: true       # OpenShift Compliance Operator
   enable_falco: true                     # Falco runtime security monitoring
   enable_gatekeeper: true                # OPA Gatekeeper policy enforcement
   
   # Individual GT Operators
   enable_pod_cleanup: true
   enable_cert_expiry_monitor: true
   enable_etcd_backup: true
   enable_cluster_intelligence: true
   enable_cost_optimization: true
   enable_namespace_quota_monitor: true
   enable_node_resource_monitor: true
   enable_pvc_storage_monitor: true
   ```
   
   **Network configuration:**
   ```yaml
   # Egress IP settings
   egress_ips:
     - "10.10.10.200"
   egress_namespace_selector:
     matchLabels:
       egress: "restricted"
   
   # NTP server
   ntp_server_host: "npt.domain.com"
   
   # Log forwarding endpoint
   log_forwarding_url: "http://10.10.10.10:9090"
   
   # Kdump configuration
   kdump_crash_kernel_memory: "256M"      # Memory reserved for crash kernel
   kdump_target_pools: ["master", "worker"]  # MachineConfigPools to enable kdump
   
   # OpenCost/Kubecost configuration
   enable_opencost_azure: true            # Azure billing integration (false for on-premises)
   opencost_excluded_namespaces: "openshift-*,kube-*,default"  # Exclude system namespaces
   ```

2. **Create `vars/vault.yml`** - Encrypted secrets
   ```bash
   ansible-vault create vars/vault.yml
   ```
   
   **Required variables:**
   ```yaml
   # LDAP Authentication
   vault_ldap_password: "your-ldap-password"
   
   # CA Certificates (PEM format)
   vault_ca_cert: |
     -----BEGIN CERTIFICATE-----
     ...
     -----END CERTIFICATE-----
   
   vault_ca_key: |
     -----BEGIN PRIVATE KEY-----
     ...
     -----END PRIVATE KEY-----
   ```
   
   **Optional variables:**
   ```yaml
   # Private Docker Registry
   vault_docker_username: "registry-username"
   vault_docker_password: "registry-password"
   
   # Microsoft Teams Notifications
   vault_teams_webhook_url: "https://outlook.webhook.office.com/..."
   
   # ServiceNow Integration
   vault_servicenow_instance: "company.service-now.com"
   vault_servicenow_user: "api_user"
   vault_servicenow_password: "api_password"
   
   # SSH Access to Nodes
   vault_ssh_public_key: "ssh-rsa AAAAB3..."
   
   # Azure Cost Management API (for Kubecost on Azure clusters)
   vault_azure_subscription_id: "your-subscription-id"
   vault_azure_tenant_id: "your-tenant-id"
   vault_azure_client_id: "your-service-principal-client-id"
   vault_azure_client_secret: "your-service-principal-secret"
   ```

3. **Create `vault_pass`** - Vault password file
   ```bash
   echo "your_vault_password" > vault_pass
   chmod 600 vault_pass
   ```

### Usage

```bash
# Run all enabled playbooks
ansible-playbook site.yml --vault-password-file vault_pass

# Run specific components
ansible-playbook site.yml --tags ldap --vault-password-file vault_pass
ansible-playbook site.yml --tags certificates --vault-password-file vault_pass
ansible-playbook site.yml --tags gt-operators --vault-password-file vault_pass

# Check mode (dry run)
ansible-playbook site.yml --check --vault-password-file vault_pass
```

---

## Components

### Core Configuration

| Playbook | Tags | Description |
|----------|------|-------------|
| **00-validate-prerequisites** | `validate` | Verify cluster connectivity and prerequisites |
| **01-cert-manager** | `cert-manager` | Install cert-manager operator for certificate management |
| **02-manage-certificate** | `certificates`, `api`, `ingress` | Configure API and Ingress certificates |
| **03-egress-ip** | `network`, `egress` | Configure OVN-Kubernetes egress IPs |
| **04-ldap-config** | `auth`, `ldap` | LDAP authentication with OAuth |
| **05-ldap-groupsync** | `groupsync` | LDAP group synchronization CronJob |
| **06-node-labeling** | `nodes`, `labeling` | Label baremetal nodes |
| **07-chrony-config** | `ntp`, `chrony` | NTP time synchronization via MachineConfig |
| **08-enable-developer-portal** | `console`, `developer` | Enable developer perspective in console |
| **09-cluster-upgrade-config** | `upgrade`, `mcp` | Configure cluster upgrade settings |
| **10-infra-scheduling** | `infrastructure`, `scheduling` | Infrastructure node configuration and workload scheduling |
| **11-image-pruning-config** | `pruning`, `registry` | Automated registry image pruning |
| **12-disable-self-provisioning** | `security`, `provisioning` | Disable project self-provisioning |
| **13-proxy-config** | `proxy`, `network` | Cluster-wide HTTP/HTTPS proxy settings |
| **14-registry-storage-config** | `registry`, `storage` | Configure image registry storage (NFS/PVC) |
| **15-rbac-config** | `rbac`, `security` | Custom ClusterRoles and ClusterRoleBindings |
| **16-worker-kubelet-config** | `kubelet`, `worker` | Worker node kubelet settings (eviction, image GC) |
| **17-dns-tuning-config** | `dns`, `tuning` | DNS performance optimization |
| **18-logging-config** | `logging` | Logging operator and log forwarding |
| **19-deploy-operators** | `operators`, `gt-operators` | Deploy GT custom operators |
| **19-deploy-operators** | `operators`, `gt-operators` | Deploy GT custom operators |
| **20-performance-tuning** | `performance`, `tuning`, `kubelet` | Performance tuning for master and worker nodes |
| **21-ssh-config** | `ssh`, `machine-config` | SSH authorized keys for node access |
| **22-haproxy-logging-config** | `haproxy`, `logging`, `ingress` | Enable HAProxy access logs for Ingress Controller |
| **23-enable-kdump** | `kdump`, `kernel`, `crash-dumps` | Enable kdump for kernel crash dump collection |
| **24-remove-worker-label-from-masters** | `node-labels`, `master`, `cleanup` | Remove worker label from master/control-plane nodes |
| **25-opencost-deployment** | `opencost`, `cost-optimization`, `monitoring` | Deploy Kubecost for cost analysis and optimization |
| **26-update-channel-config** | `update`, `channel` | Configure cluster update channel (stable/fast/candidate) |
| **27-compliance-operator** | `compliance`, `security` | Install OpenShift Compliance Operator for security scanning |
| **28-falco-security** | `falco`, `security`, `runtime` | Deploy Falco for runtime security monitoring and threat detection |
| **29-gatekeeper-policies** | `gatekeeper`, `policies`, `opa` | Install OPA Gatekeeper and deploy policy enforcement |

### GT Operators

Custom monitoring and automation operators deployed to `gt-operators` namespace:

| Operator | Type | Schedule | Description |
|----------|------|----------|-------------|
| **pod-cleanup** | CronJob | Every 6 hours | Delete completed/failed pods |
| **cert-expiry-monitor** | CronJob | Daily at 9 AM | Monitor certificate expiration |
| **etcd-backup** | CronJob | Daily at 2 AM | Backup etcd to master nodes |
| **namespace-quota-monitor** | CronJob | Every 6 hours | Monitor namespace resource quotas |
| **node-resource-monitor** | CronJob | Every 30 min | Monitor node resource usage |
| **pvc-storage-monitor** | CronJob | Hourly | Monitor PVC storage usage |
| **cluster-intelligence** | Deployment | Continuous | Cluster health and recommendations |
| **cost-optimization** | Deployment | Continuous | Resource optimization suggestions |

**Features:**
- Private registry support (Docker Hub, Quay.io, etc.)
- Teams webhook notifications
- Security Context Constraints (SCC) auto-configured
- Configurable via `vars/all_vars.yaml`

---

## Key Configuration

### Egress IP Configuration

Control outbound traffic from specific namespaces using dedicated IP addresses:

```yaml
# Enable egress IP feature
enable_egress_ip: true

# Static IP addresses for egress traffic (highly available across workers)
egress_ips:
  - "10.19.86.200"

# Which namespaces use egress IPs
egress_namespace_selector:
  matchLabels:
    egress: "restricted"

# Optional: Pod-level selector
# egress_pod_selector:
#   matchLabels:
#     app: "my-app"
```

**How it works:**
1. All worker nodes automatically labeled with `k8s.ovn.org/egress-assignable`
2. Namespaces matching the selector route traffic through specified IPs
3. High availability: IPs automatically move between workers if nodes fail

### Registry Storage Configuration

```yaml
enable_registry_storage: false
registry_storage_type: "nfs"  # Options: "nfs" or "pvc"

# NFS storage
nfs_server: "nfs.example.com"
registry_nfs_path: "/exports/registry"

# PVC storage
registry_pvc_name: "image-registry-storage"
registry_storage_size: "100Gi"
```

### Image Pruning Configuration

```yaml
enable_image_pruning: true
image_pruner_schedule: "0 17 * * *"  # Daily at 5 PM
image_pruner_keep_tag_revisions: 3   # Keep 3 versions of each tag
image_pruner_keep_younger_than: 60   # Keep images < 60 hours old
```

### Log Forwarding Configuration

```yaml
enable_log_forwarding: true
log_forwarding_url: "http://10.10.10.10:9090"
log_forwarding_timeout: 60
log_forwarding_batch_max_bytes: 102400
log_forwarding_batch_timeout_secs: 5
```

### NTP/Chrony Configuration

```yaml
enable_chrony: true
ntp_server_host: "ntp.domain.lan"

# Optional fallback servers
ntp_fallback_servers:
  - "pool.ntp.org"
  - "time.cloudflare.com"
```

### Infrastructure Nodes (`vars/all_vars.yaml`)

```yaml
# Enable dedicated infrastructure nodes
enable_infra: false  # Set to true to configure infra nodes

# Specify infrastructure nodes (required if enable_infra: true)
infra_nodes:
  - "worker-01"
  - "worker-02"

# Components scheduled on infra nodes (when enabled):
# - Ingress controllers
# - Image registry
# - Monitoring stack
# - Logging stack
```

### Kubelet Configuration

Worker and infrastructure nodes use **static eviction thresholds** with optional system resource reservation:

```yaml
# Static eviction thresholds (hard-coded in templates)
evictionHard:
  memory.available: "500Mi"
  nodefs.available: "10%"
  nodefs.inodesFree: "5%"
  imagefs.available: "15%"
  imagefs.inodesFree: "5%"

evictionSoft:
  memory.available: "1Gi"
  nodefs.available: "15%"
  nodefs.inodesFree: "10%"
  imagefs.available: "20%"
  imagefs.inodesFree: "10%"

evictionSoftGracePeriod:
  memory.available: "1m30s"
  nodefs.available: "1m30s"
  nodefs.inodesFree: "1m30s"
  imagefs.available: "1m30s"
  imagefs.inodesFree: "1m30s"

imageGCHighThresholdPercent: 80
imageGCLowThresholdPercent: 60
imageMinimumGCAge: "5m"

# Optional: System reserved resources (configurable in all_vars.yaml)
kubelet_system_reserved: "yes"
kubelet_reserved_cpu_worker: "500m"
kubelet_reserved_memory_worker: "1Gi"
kubelet_max_pods: 250
```

**Files:**
- `templates/configs/machine-configs/worker-kubelet-config.yaml`
- `templates/configs/machine-configs/infra-kubelet-config.yaml`

### RBAC Configuration

Two ClusterRoles available:

**gtops-role** - Full developer + operator permissions
- Namespace resources: pods, deployments, services, routes, etc.
- Cluster resources: nodes (view), operators, FluxCD
- Assigned to: `cluster_admin_groups` and `gtops_additional_groups`

**gtops-operators-readonly** - View-only access
- Operators, cluster configs, namespaces, nodes (read-only)
- Assigned to: `cluster_admin_groups` and `gtops_additional_groups`

```yaml
# Configure in vars/all_vars.yaml
cluster_admin_groups:
  - "OpenShift-Admin-Group"
gtops_additional_groups:
  - "OpenShift-Developers-Group"
```

### Private Registry

```yaml
# Enable private registry for GT operators
enable_registry_secret: true
docker_registry_url: "docker.io"
docker_registry_email: "user@example.com"

# Credentials in vars/vault.yml:
vault_docker_username: "your-username"
vault_docker_password: "your-password"
```

### OpenCost/Kubecost Configuration

Kubecost provides cost visibility, optimization recommendations, and chargeback for Kubernetes clusters.

```yaml
# Enable Kubecost deployment
enable_opencost: true

# Azure billing integration (for accurate Azure costs)
enable_opencost_azure: true  # Set to false for on-premises clusters

# Exclude system namespaces from cost calculations
opencost_excluded_namespaces: "openshift-*,kube-*,default"
```

**Azure Integration** (required for accurate costs on Azure):

1. Create service principal with Cost Management Reader role:
   ```bash
   az ad sp create-for-rbac --name "kubecost-cost-reader" \
     --role "Cost Management Reader" \
     --scopes /subscriptions/YOUR_SUBSCRIPTION_ID
   ```

2. Add credentials to `vars/vault.yml`:
   ```yaml
   vault_azure_subscription_id: "xxxx-..."
   vault_azure_tenant_id: "xxxx-..."
   vault_azure_client_id: "xxxx-..."
   vault_azure_client_secret: "xxxxx..."
   ```

**On-Premises Clusters:**
- Set `enable_opencost_azure: false`
- Kubecost uses custom pricing (CPU: $0.03/core-hour, RAM: $0.004/GB-hour, Storage: $0.04/GB-month)
- Edit `templates/k8s/kubecost/kubecost-instance.yaml` to adjust pricing

**Features:**
- 📊 Cost breakdown by namespace, pod, deployment, label
- 💰 Idle resource detection and waste analysis
- 📈 Cost trends and forecasting
- ⚙️ Right-sizing recommendations
- 📋 Chargeback reports

**Access:**
- UI: `https://kubecost-ui-kubecost.apps.<cluster-domain>`
- Wait 10-15 minutes after deployment for initial data collection
- Navigate to "Allocations" tab for cost breakdown
- Check "Savings" tab for optimization recommendations

### Falco Runtime Security Configuration

Falco provides real-time runtime security monitoring by detecting anomalous behavior in containers and Kubernetes clusters.

```yaml
# Enable Falco deployment
enable_falco: true
```

**Deployed Components:**
- Falco DaemonSet (eBPF driver for kernel monitoring)
- Falcosidekick (Alert aggregation and forwarding)
- Falcosidekick UI (Web-based alert visualization)

**Production Security Rules (20 rules):**
1. **Container Security**: Unauthorized package management, interactive shells, shell spawning
2. **Privilege Escalation**: Sudo execution detection
3. **Persistence**: Cron job modifications
4. **Credential Access**: Sensitive file access (/etc/passwd, /etc/shadow), K8s service account tokens
5. **Network Security**: Reverse shells, suspicious outbound connections, port scanning
6. **File System**: Binary writes in containers, cryptomining detection, suspicious downloads
7. **Kubernetes**: kubectl/oc execution, privileged containers, SCC violations
8. **Data Exfiltration**: Database dump detection
9. **Compliance**: SSH server spawning

**Features:**
- JSON-formatted logs with MITRE ATT&CK tags
- Automatic namespace filtering (excludes openshift-*, kube-system, falco, monitoring, default)
- Integration with centralized logging (if `enable_log_forwarding: true`)
- Alert priority levels: ERROR, WARNING, NOTICE

**Access:**
- Falcosidekick UI: `https://falcosidekick-ui-falco.apps.<cluster-domain>`
- View alerts: `oc logs -n falco -l app.kubernetes.io/name=falco -c falco`
- Check rules: `oc exec -n falco <pod> -c falco -- falco --list`

**Log Forwarding:**
Falco logs automatically included in centralized logging when `enable_log_forwarding: true`.
Alerts available in JSON format with fields: `rule`, `priority`, `output`, `tags`, `k8s.ns.name`, `k8s.pod.name`

### OPA Gatekeeper Policy Enforcement

Gatekeeper provides policy-based admission control using Open Policy Agent (OPA) and Rego language.

```yaml
# Enable Gatekeeper deployment
enable_gatekeeper: true
```

**Deployed Components:**
- Gatekeeper Operator (v3.19.1+)
- Gatekeeper Controller Manager (2 replicas for HA)
- Gatekeeper Audit Controller (periodic compliance scanning)
- Validating Admission Webhook (real-time policy enforcement)

**Production Policies (3 constraint templates):**

1. **K8sRequiredLabels** - Enforce required labels on namespaces
   - Required labels: `owner`, `environment`
   - Exemptions: kube-*, openshift-*, default
   - Enforcement: Deny namespace creation without required labels

2. **K8sBlockPrivileged** - Block privileged containers
   - Blocks: Privileged containers and init containers
   - Exemptions: kube-system, openshift-*
   - Enforcement: Deny pod creation with `privileged: true`

3. **K8sRequireResourceLimits** - Enforce resource limits on workloads
   - Required: CPU and memory limits
   - Applies to: Deployments, StatefulSets, DaemonSets
   - Exemptions: kube-system, openshift-*

**Features:**
- Cluster-wide policy enforcement (AllNamespaces mode)
- Audit mode available (`enforcementAction: dryrun`)
- Violation reporting and tracking
- Namespace-based exemptions
- Real-time admission control

**Access:**
- View policies: `oc get constrainttemplates`
- View constraints: `oc get constraints --all-namespaces`
- View violations: `oc get k8srequiredlabels namespace-must-have-labels -o yaml`
- Audit logs: `oc logs -n openshift-gatekeeper-system -l control-plane=audit-controller`

**Deployment Location:**
- Namespace: `openshift-gatekeeper-system`
- Webhook pods: Deployed in `openshift-gatekeeper-system` (not `gatekeeper-system`)
- Mode: AllNamespaces (cluster-wide enforcement)

---

## Troubleshooting

### Common Issues

**RBAC - User cannot list nodes**
```bash
# Verify ClusterRoleBinding exists
oc get clusterrolebinding gtops-binding

# Check user's group membership
oc get groups | grep <username>

# Create binding if missing
oc create clusterrolebinding gtops-binding \
  --clusterrole=gtops-role \
  --group=OpenShift-Admins
```

**Operators - Pod stuck in Pending**
```bash
# Check events
oc get events -n gt-operators --sort-by='.lastTimestamp'

# Common issues:
# - Missing SCC: Already configured in playbook (anyuid/privileged)
# - Node selector/taints: etcd-backup needs master node toleration
# - ImagePullBackOff: Check registry secret

# Verify SCC grants
oc get scc privileged -o yaml | grep -A 10 users
```

**LDAP - Authentication fails**
```bash
# Check OAuth pods
oc get pods -n openshift-authentication

# Verify LDAP secret
oc get secret ldap-secret -n openshift-config

# Check authentication operator
oc get co authentication
oc logs -n openshift-authentication <pod-name>
```

**Certificates - Not applied**
```bash
# Check cert-manager certificates
oc get certificate -A

# Verify ClusterIssuer
oc get clusterissuer

# Check cluster operators
oc get co kube-apiserver ingress
```

**Egress IP - Not routing through configured IP**
```bash
# Verify worker node labels
oc get nodes -l k8s.ovn.org/egress-assignable

# Check EgressIP resource
oc get egressip cluster-egress -o yaml

# Verify namespace has correct label
oc get ns <namespace> --show-labels | grep egress

# Check EgressIP status
oc get egressip
```

**Image Pruning - Not running**
```bash
# Check ImagePruner configuration
oc get imagepruner cluster -o yaml

# Verify CronJob
oc get cronjob -n openshift-image-registry

# Check recent jobs
oc get jobs -n openshift-image-registry
```

**Log Forwarding - Logs not forwarded**
```bash
# Check ClusterLogForwarder
oc get clusterlogforwarder -n openshift-logging

# Verify collector pods
oc get pods -n openshift-logging | grep collector

# Check forwarder logs
oc logs -n openshift-logging -l component=collector
```

**Chrony/NTP - Time sync issues**
```bash
# Check MachineConfig
oc get mc | grep chrony

# Verify nodes have config
oc get nodes
oc debug node/<node-name> -- chroot /host chronyc sources

# Check MachineConfigPool status
oc get mcp
```

**Infrastructure Nodes - Workloads not scheduling**
```bash
# Verify infra node labels
oc get nodes -l node-role.kubernetes.io/infra

# Check MachineConfigPool
oc get mcp infra

# Verify component scheduling
oc get ingresscontroller default -n openshift-ingress-operator -o yaml | grep -A 5 nodePlacement
oc get config cluster -n openshift-image-registry -o yaml | grep -A 5 nodeSelector
```

**SSH Keys - Cannot access nodes**
```bash
# Check MachineConfig
oc get mc | grep ssh

# Verify rendered config
oc get mc 99-master-ssh-authorized-keys -o yaml
oc get mc 99-worker-ssh-authorized-keys -o yaml

# Test SSH access
ssh core@<node-ip>
```

### Debug Commands

```bash
# Verbose Ansible output
ansible-playbook site.yml -vvv --vault-password-file vault_pass

# Check specific component
oc get <resource> -A | grep <component>

# View recent events
oc get events -A --sort-by='.lastTimestamp' | tail -20

# Operator logs
oc logs -n gt-operators <pod-name> --tail=50
```

**OpenCost/Kubecost - No cost data showing**
```bash
# Check pods are running
oc get pods -n kubecost

# Verify Azure integration (if enabled)
oc get secret cloud-integration -n kubecost

# Check logs for errors
oc logs -n kubecost deployment/kubecost-instance-cost-analyzer -c cost-model --tail=50

# Verify Prometheus connection
oc logs -n kubecost deployment/kubecost-instance-cost-analyzer -c cost-model | grep -i prometheus

# Check route
oc get route kubecost-ui -n kubecost

# Common issues:
# - Azure costs showing $0.00: Wait 10-15 min for data collection or verify Azure credentials
# - Prometheus errors: Check Thanos Querier route is accessible
# - On-premises: Set enable_opencost_azure: false and verify custom pricing in instance.yaml
```

**Kdump - Verify kernel crash dump configuration**
```bash
# Check MachineConfig status
oc get mc | grep kdump

# Monitor MachineConfigPool update progress
oc get mcp master worker -w

# Verify kdump is enabled on nodes (after MCP update completes)
oc debug node/<node-name> -- chroot /host systemctl status kdump

# Check crash kernel memory reservation
oc debug node/<node-name> -- chroot /host cat /proc/cmdline | grep crashkernel

# View kdump configuration
oc debug node/<node-name> -- chroot /host cat /etc/kdump.conf

# Check crash dump location
oc debug node/<node-name> -- chroot /host ls -lh /var/crash/
```

**Falco - Runtime security not detecting events**
```bash
# Check Falco pods are running
oc get pods -n falco

# Verify eBPF driver is loaded
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | grep -i "driver"

# Check for rule loading errors
oc logs -n falco -l app.kubernetes.io/name=falco -c falco | grep -i "error\|warn"

# Verify production rules are loaded
oc exec -n falco <pod-name> -c falco -- falco --list | grep -E "(Privileged|Package Management|Sensitive)"

# Test alert generation (trigger a rule)
oc exec -n <namespace> <pod-name> -- cat /etc/passwd

# Check Falcosidekick UI
oc get route -n falco falcosidekick-ui

# Verify logs in centralized logging
oc logs -n openshift-logging -l app.kubernetes.io/component=collector | grep falco

# Common issues:
# - GitHub rate limit (429): Delete failing pod, will retry
# - No alerts: Check namespace is not in exclusion list (openshift-*, kube-system, etc.)
# - eBPF errors: Check kernel version compatibility
```

**Gatekeeper - Policies not enforcing**
```bash
# Check operator and webhook pods
oc get pods -n openshift-gatekeeper-system

# Verify constraint templates are created
oc get constrainttemplates

# Check if CRDs are established
oc get crd | grep -E "(k8srequiredlabels|k8sblockprivileged|k8srequireresourcelimits)"

# View constraint status and violations
oc get k8srequiredlabels namespace-must-have-labels -o yaml

# Check webhook configuration
oc get validatingwebhookconfiguration gatekeeper-validating-webhook-configuration

# Test policy enforcement
oc create namespace test-namespace  # Should fail without required labels

# View audit logs
oc logs -n openshift-gatekeeper-system -l control-plane=audit-controller --tail=50

# Common issues:
# - "OwnNamespace InstallModeType not supported": Use AllNamespaces mode (spec: {})
# - Webhook blocking operator namespace: Add openshift-gatekeeper-system to exempt list
# - Policies not enforcing: Check enforcementAction is not set to "dryrun"
# - CRDs not created: Wait 10-15 seconds after template creation
```

---

### Kdump Configuration

Enable kernel crash dump collection for debugging kernel panics and crashes:

```yaml
# Enable kdump
enable_kdump: true

# Memory reserved for crash kernel (default: 256M)
kdump_crash_kernel_memory: "256M"

# Target node pools (default: master and worker)
kdump_target_pools: ["master", "worker"]

# Optional: Custom crash dump path (default: /var/crash)
# kdump_path: "/var/crash"

# Optional: Disk quota in MB to prevent filling disk (default: 2048MB = 2GB)
# Older dumps automatically removed when quota exceeded
# kdump_disk_quota: "2048"

# Optional: Action after dump completes (default: reboot)
# kdump_default_action: "reboot"   # HA production: auto-reboot after dump
# kdump_default_action: "poweroff" # Conservative: manual investigation required
```

**Production best practices:**
- **Compression enabled**: Uses `makedumpfile -l -d 31` for significant space savings
  - `-l`: Compression (typically 70-90% reduction)
  - `-d 31`: Excludes free pages, cache, user data (only kernel data captured)
- **Disk quota protection**: Default 2GB limit prevents disk exhaustion
  - Automatically removes oldest dumps when quota exceeded
  - Recommended: 2-4GB depending on available disk space
- **Auto-reboot after dump**: Default behavior for HA clusters
  - Node captures dump and automatically reboots
  - Kubernetes will reschedule workloads during brief downtime
  - Change to `poweroff` if you need manual investigation before restart
- **Local storage**: Dumps saved to `/var/crash` on local disk

**Important notes:**
- This configuration requires node reboots (rolling update via MachineConfigPool)
- Each node in the target pools will be drained, rebooted, and returned to service
- Update process can take 30-60 minutes depending on cluster size
- Monitor progress: `oc get mcp -w`
- Compressed dumps typically 200-500MB (vs 2-8GB uncompressed)

**Verification:**
```bash
# Wait for MachineConfigPools to finish updating
oc wait --for=condition=Updated=True --timeout=30m mcp/master mcp/worker

# Verify kdump service is active
oc debug node/<node-name> -- chroot /host systemctl is-active kdump
```

---

### Security Context Constraints

GT Operators automatically configured with appropriate SCCs:

- **anyuid**: Most operators (cert-expiry-monitor, namespace-quota-monitor, etc.)
- **privileged**: etcd-backup (requires hostNetwork, hostPID, hostPath)

Applied automatically during operator deployment.

---

## Requirements

- **OpenShift**: 4.14 - 4.18+ (OVN-Kubernetes networking)
- **Ansible**: 2.15.0+
- **Python**: 3.9+ with `kubernetes`, `openshift`, `PyYAML` modules
- **Access**: cluster-admin permissions via KUBECONFIG
