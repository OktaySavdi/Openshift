# Cert-Manager Operator Installation - OpenShift CLI Procedure

## Prerequisites
- OpenShift cluster with cluster-admin access
- `oc` CLI logged in

## Overview
Install Red Hat cert-manager operator for automated certificate management and Let's Encrypt integration.

## Step 1: Create Namespace
```bash
oc create namespace cert-manager-operator
```

## Step 2: Create OperatorGroup
```yaml
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: cert-manager-operator
  namespace: cert-manager-operator
spec:
  targetNamespaces:
  - cert-manager-operator
```
```bash
oc apply -f operatorgroup.yaml   # (If saved) or inline with heredoc
```
```bash
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: cert-manager-operator
  namespace: cert-manager-operator
spec:
  targetNamespaces:
  - cert-manager-operator
EOF
```

## Step 3: Create Subscription
```yaml
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-cert-manager-operator
  namespace: cert-manager-operator
spec:
  channel: stable-v1
  name: openshift-cert-manager-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
```
```bash
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-cert-manager-operator
  namespace: cert-manager-operator
spec:
  channel: stable-v1
  name: openshift-cert-manager-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
EOF
```

## Step 4: Wait for Operator
```bash
oc get csv -n cert-manager-operator -w
# Or
oc get csv -n cert-manager-operator
```

## Step 5: Verify Installation
```bash
oc get pods -n cert-manager
oc get deployments -n cert-manager
oc get crd | grep cert-manager
```

## Verification
```bash
oc get csv -n cert-manager-operator
oc get pods -n cert-manager
oc api-resources | grep cert-manager
```

## Troubleshooting
```bash
# Operator not installing
oc get installplan -n cert-manager-operator
oc describe subscription openshift-cert-manager-operator -n cert-manager-operator

# Pods not running
oc get events -n cert-manager --sort-by='.lastTimestamp'
oc logs -n cert-manager deployment/cert-manager
```

## Uninstall
```bash
oc delete subscription openshift-cert-manager-operator -n cert-manager-operator
oc delete csv -n cert-manager-operator -l operators.coreos.com/openshift-cert-manager-operator.cert-manager-operator=
oc delete namespace cert-manager-operator
```

## Notes
- Operator creates `cert-manager` namespace automatically
- Installs webhook, controller, and cainjector
- Required for automated certificate management
- Supports Let's Encrypt, self-signed, and CA issuers
