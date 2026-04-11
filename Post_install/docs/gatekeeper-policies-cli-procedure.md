# OPA GATEKEEPER - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- OpenShift 4.11 or higher

## OVERVIEW
Deploy OPA Gatekeeper Operator for policy-based admission control and governance.

## STEP 1: INSTALL GATEKEEPER OPERATOR

```bash
# Create namespace
oc create namespace openshift-gatekeeper-system

# Create operator group (AllNamespaces mode for cluster-wide enforcement)
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: gatekeeper-operator
  namespace: openshift-gatekeeper-system
spec: {}
  # Empty spec = AllNamespaces mode (required for Gatekeeper to watch cluster-wide)
EOF

# Create subscription
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: gatekeeper-operator-product
  namespace: openshift-gatekeeper-system
spec:
  channel: stable
  installPlanApproval: Automatic
  name: gatekeeper-operator-product
  source: redhat-operators
  sourceNamespace: openshift-marketplace
EOF

# Verify operator installation
oc get csv -n openshift-gatekeeper-system
oc get pods -n openshift-gatekeeper-system
```

## STEP 2: CREATE GATEKEEPER INSTANCE

```bash
# Create Gatekeeper instance
cat <<EOF | oc apply -f -
apiVersion: operator.gatekeeper.sh/v1alpha1
kind: Gatekeeper
metadata:
  name: gatekeeper
spec:
  audit:
    auditInterval: 60s
    auditChunkSize: 500
    auditFromCache: Automatic
    constraintViolationLimit: 20
    logLevel: INFO
    replicas: 1
  validatingWebhook: Enabled
  mutatingWebhook: Disabled
  webhook:
    replicas: 2
    failurePolicy: Ignore
    namespaceSelector:
      matchExpressions:
        - key: admission.gatekeeper.sh/ignore
          operator: DoesNotExist
  resources:
    limits:
      cpu: 1000m
      memory: 512Mi
    requests:
      cpu: 100m
      memory: 256Mi
EOF

# Wait for Gatekeeper to be ready
oc get gatekeeper gatekeeper
oc get pods -n openshift-gatekeeper-system
```

## STEP 3: CREATE CONSTRAINT TEMPLATES

### Require Labels Template
```bash
cat <<EOF | oc apply -f -
apiVersion: templates.gatekeeper.sh/v1
kind: ConstraintTemplate
metadata:
  name: k8srequiredlabels
spec:
  crd:
    spec:
      names:
        kind: K8sRequiredLabels
      validation:
        openAPIV3Schema:
          type: object
          properties:
            labels:
              type: array
              items:
                type: string
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package k8srequiredlabels
        
        violation[{"msg": msg, "details": {"missing_labels": missing}}] {
          provided := {label | input.review.object.metadata.labels[label]}
          required := {label | label := input.parameters.labels[_]}
          missing := required - provided
          count(missing) > 0
          msg := sprintf("Missing required labels: %v", [missing])
        }
EOF
```

### Block Privileged Containers Template
```bash
cat <<EOF | oc apply -f -
apiVersion: templates.gatekeeper.sh/v1
kind: ConstraintTemplate
metadata:
  name: k8sblockprivileged
spec:
  crd:
    spec:
      names:
        kind: K8sBlockPrivileged
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package k8sblockprivileged
        
        violation[{"msg": msg}] {
          container := input.review.object.spec.containers[_]
          container.securityContext.privileged
          msg := sprintf("Privileged container not allowed: %v", [container.name])
        }
EOF
```

## STEP 4: CREATE CONSTRAINTS

### Enforce Required Labels
```bash
cat <<EOF | oc apply -f -
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sRequiredLabels
metadata:
  name: namespace-must-have-labels
spec:
  match:
    kinds:
      - apiGroups: [""]
        kinds: ["Namespace"]
    excludedNamespaces:
      - kube-system
      - openshift-*
      - default
  parameters:
    labels:
      - "owner"
      - "environment"
EOF
```

### Block Privileged Containers
```bash
cat <<EOF | oc apply -f -
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sBlockPrivileged
metadata:
  name: block-privileged-containers
spec:
  match:
    kinds:
      - apiGroups: [""]
        kinds: ["Pod"]
    excludedNamespaces:
      - kube-system
      - openshift-*
EOF
```

## STEP 5: VIEW POLICIES

```bash
# List all constraint templates
oc get constrainttemplates.templates.gatekeeper.sh

# List all constraints
oc get constraints --all-namespaces

# View specific constraint
oc describe k8srequiredlabels namespace-must-have-labels
```

## STEP 6: CHECK VIOLATIONS

```bash
# View all violations
oc get constraints -o json | \
  jq -r '.items[] | select(.status.totalViolations > 0) | "\(.kind)/\(.metadata.name): \(.status.totalViolations) violations"'

# View detailed violations for specific constraint
oc get k8srequiredlabels namespace-must-have-labels -o yaml

# Get violation details
oc get k8srequiredlabels namespace-must-have-labels -o jsonpath='{.status.violations}'
```

## STEP 7: TEST POLICY ENFORCEMENT

```bash
# Test 1: Create namespace without required labels (should fail)
oc create namespace test-namespace
# Expected: Admission webhook denied

# Test 2: Create namespace with required labels (should succeed)
oc create namespace test-namespace \
  --labels="owner=platform-team,environment=dev"

# Test 3: Try to create privileged pod (should fail)
cat <<EOF | oc apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: privileged-test
  namespace: default
spec:
  containers:
  - name: test
    image: busybox
    securityContext:
      privileged: true
EOF
# Expected: Admission webhook denied
```

## STEP 8: DRY-RUN MODE (AUDIT ONLY)

```bash
# Set constraint to audit-only mode
cat <<EOF | oc apply -f -
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sBlockPrivileged
metadata:
  name: block-privileged-containers
spec:
  enforcementAction: dryrun  # Audit only, don't block
  match:
    kinds:
      - apiGroups: [""]
        kinds: ["Pod"]
EOF

# View audit results
oc get k8sblockprivileged block-privileged-containers -o yaml
```

## ADDITIONAL CONSTRAINT TEMPLATES

### Require Resource Limits
```bash
cat <<EOF | oc apply -f -
apiVersion: templates.gatekeeper.sh/v1
kind: ConstraintTemplate
metadata:
  name: k8srequireresourcelimits
spec:
  crd:
    spec:
      names:
        kind: K8sRequireResourceLimits
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package k8srequireresourcelimits
        
        violation[{"msg": msg}] {
          container := input.review.object.spec.containers[_]
          not container.resources.limits.cpu
          msg := sprintf("Container %v must have CPU limit", [container.name])
        }
        
        violation[{"msg": msg}] {
          container := input.review.object.spec.containers[_]
          not container.resources.limits.memory
          msg := sprintf("Container %v must have memory limit", [container.name])
        }
EOF
```

## ANSIBLE AUTOMATION

```bash
# Run Gatekeeper deployment playbook
cd /path/to/Manage_Cluster
ansible-playbook playbooks/29-gatekeeper-policies.yml --vault-password-file=vault_pass

# Variables in vars/all_vars.yaml:
# enable_gatekeeper: true
# gatekeeper_audit_interval: "60"
# gatekeeper_mutation_enabled: false
```

## TROUBLESHOOTING

```bash
# Check Gatekeeper pods (deployed in openshift-gatekeeper-system)
oc get pods -n openshift-gatekeeper-system

# View controller logs
oc logs -n openshift-gatekeeper-system -l control-plane=controller-manager

# View audit logs
oc logs -n openshift-gatekeeper-system -l control-plane=audit-controller

# Check webhook configuration
oc get validatingwebhookconfiguration gatekeeper-validating-webhook-configuration

# Verify CRDs
oc get crd | grep gatekeeper
```

## COMMON ISSUES

### OperatorGroup InstallMode Error
**Issue**: "OwnNamespace InstallModeType not supported"
**Solution**: Use AllNamespaces mode by removing targetNamespaces from OperatorGroup spec
```bash
oc delete operatorgroup gatekeeper-operator -n openshift-gatekeeper-system
# Then recreate with empty spec (see Step 1)
```

### Webhook Blocking Operator Namespace
**Issue**: "admission webhook denied the request: Only exempt namespace can have the admission.gatekeeper.sh/ignore label"
**Solution**: Add openshift-gatekeeper-system to webhook exempt list
```bash
oc patch validatingwebhookconfiguration gatekeeper-validating-webhook-configuration \
  --type='json' -p='[{"op": "add", "path": "/webhooks/1/namespaceSelector/matchExpressions/0/values/-", "value": "openshift-gatekeeper-system"}]'
```

### Webhook Not Working
```bash
# Check webhook service
oc get svc -n openshift-gatekeeper-system gatekeeper-webhook-service

# Verify certificates
oc get secret -n openshift-gatekeeper-system gatekeeper-webhook-server-cert

# Restart Gatekeeper operator
oc rollout restart deployment -n openshift-gatekeeper-system gatekeeper-operator-controller
```

### Policies Not Enforcing
```bash
# Check if constraint template is established
oc get constrainttemplate <template-name> -o jsonpath='{.status.created}'

# Verify constraint is enforced (not dryrun)
oc get <constraint-kind> <constraint-name> -o jsonpath='{.spec.enforcementAction}'
```

## MONITORING GATEKEEPER

```bash
# Get Gatekeeper metrics
oc port-forward -n openshift-gatekeeper-system svc/gatekeeper-webhook-service 8888:443

# Access metrics
curl -k https://localhost:8888/metrics
```

## USEFUL COMMANDS

```bash
# Count total violations
oc get constraints -o json | \
  jq '[.items[].status.totalViolations // 0] | add'

# Export all constraints
oc get constraints --all-namespaces -o yaml > gatekeeper-constraints.yaml

# List all Rego policies
oc get constrainttemplates -o json | \
  jq -r '.items[].spec.targets[].rego'
```

## REFERENCE
- Gatekeeper Documentation: https://open-policy-agent.github.io/gatekeeper/
- OPA Policy Language: https://www.openpolicyagent.org/docs/latest/policy-language/
- Constraint Template Library: https://github.com/open-policy-agent/gatekeeper-library
