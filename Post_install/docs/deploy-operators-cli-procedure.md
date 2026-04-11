# Deploy Operators - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- OperatorHub catalog sources available

## Overview
Install operators from OperatorHub using `Subscription` and `OperatorGroup` resources.

## Step 1: List Available Operators
```bash
oc get packagemanifests -n openshift-marketplace
oc get packagemanifests -n openshift-marketplace | grep mongodb
oc describe packagemanifest mongodb-enterprise -n openshift-marketplace
oc get catalogsources -n openshift-marketplace
```

## Step 2: Create Namespace
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: mongodb-operator
  labels:
    openshift.io/cluster-monitoring: "true"
```
```bash
oc apply -f - <<'EOF'
apiVersion: v1
kind: Namespace
metadata:
  name: mongodb-operator
  labels:
    openshift.io/cluster-monitoring: "true"
EOF
```

## Step 3: Create OperatorGroup
Namespace-scoped:
```yaml
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: mongodb-operatorgroup
  namespace: mongodb-operator
spec:
  targetNamespaces:
  - mongodb-operator
```
Cluster-scoped:
```yaml
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: global-operators
  namespace: openshift-operators
spec: {}
```

## Step 4: Create Subscription
Specific version:
```yaml
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: mongodb-enterprise
  namespace: mongodb-operator
spec:
  channel: stable
  name: mongodb-enterprise
  source: certified-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
  startingCSV: mongodb-enterprise.v1.21.0
```
Latest version:
```yaml
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: mongodb-enterprise
  namespace: mongodb-operator
spec:
  channel: stable
  name: mongodb-enterprise
  source: certified-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
```

## Step 5: Verify Installation
```bash
oc get subscription -n mongodb-operator
oc get csv -n mongodb-operator
oc get pods -n mongodb-operator
oc get installplan -n mongodb-operator
oc describe csv mongodb-enterprise.v1.21.0 -n mongodb-operator
```

## Step 6: Manual Approval (If Required)
```bash
oc get installplan -n mongodb-operator
oc patch installplan <install-plan-name> -n mongodb-operator --type merge -p '{"spec":{"approved":true}}'
```

## Common Operators
```yaml
# Logging
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cluster-logging
  namespace: openshift-logging
spec:
  channel: stable
  name: cluster-logging
  source: redhat-operators
  sourceNamespace: openshift-marketplace
```
```yaml
# Pipelines (Tekton)
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-pipelines-operator
  namespace: openshift-operators
spec:
  channel: latest
  name: openshift-pipelines-operator-rh
  source: redhat-operators
  sourceNamespace: openshift-marketplace
```
```yaml
# GitOps (ArgoCD)
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-gitops-operator
  namespace: openshift-operators
spec:
  channel: latest
  name: openshift-gitops-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
```
```yaml
# Cert-Manager
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cert-manager
  namespace: cert-manager-operator
spec:
  channel: stable
  name: cert-manager
  source: redhat-operators
  sourceNamespace: openshift-marketplace
```
```yaml
# Prometheus
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: prometheus
  namespace: openshift-operators
spec:
  channel: beta
  name: prometheus
  source: community-operators
  sourceNamespace: openshift-marketplace
```

## Troubleshooting
```bash
oc describe subscription <subscription-name> -n <namespace>
oc get events -n <namespace> --sort-by='.lastTimestamp'
oc get csv -n <namespace>
oc describe csv <csv-name> -n <namespace>
oc logs -n <namespace> deployment/<operator-deployment>
oc get installplan -n <namespace>
oc describe installplan <plan-name> -n <namespace>
oc get catalogsource -n openshift-marketplace
oc describe catalogsource <catalog-name> -n openshift-marketplace
```

## Upgrade Operator
Automatic: happens with `installPlanApproval: Automatic`.
Manual:
```bash
oc get subscription <subscription-name> -n <namespace> -o yaml
oc get installplan -n <namespace>
oc patch installplan <plan-name> -n <namespace> --type merge -p '{"spec":{"approved":true}}'
```

## Uninstall Operator
```bash
oc delete subscription <subscription-name> -n <namespace>
oc delete csv <csv-name> -n <namespace>
oc delete operatorgroup <operatorgroup-name> -n <namespace>
oc get crd | grep <operator-domain>  # then optionally
oc delete crd <crd-name>
oc delete namespace <namespace>
```

## Configuration Options
```yaml
spec:
  installPlanApproval: Manual
spec:
  installPlanApproval: Automatic
spec:
  channel: stable
spec:
  startingCSV: operator-name.v1.2.3
spec:
  source: community-operators
```

## Install Modes
```yaml
# OwnNamespace
spec:
  targetNamespaces:
  - my-namespace
# SingleNamespace
spec:
  targetNamespaces:
  - watched-namespace
# MultiNamespace
spec:
  targetNamespaces:
  - namespace1
  - namespace2
# AllNamespaces
spec: {}
```

## Best Practices
- Dedicated namespace per operator
- Automatic approval for dev/test
- Manual approval for production
- Document installed operators
- Test upgrades in non-prod
- Monitor operator health
- Understand RBAC permissions
- Check OpenShift version compatibility
- Review release notes

## Notes
- OLM manages lifecycle
- OperatorGroup defines scope
- Subscription tracks desired state
- CSV is installed version
- CRDs created automatically
- Some operators require specific namespaces
