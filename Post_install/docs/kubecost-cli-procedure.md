# Kubecost/OpenCost Manual Installation - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- `oc` CLI
- Optional Azure credentials for billing integration

## Step 1: Namespace
```bash
oc create namespace kubecost
oc label namespace kubecost openshift.io/cluster-monitoring=true
```

## Step 2: Prometheus RBAC
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: kubecost-prometheus
  namespace: kubecost
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: kubecost-prometheus-query
rules:
- apiGroups: [""]
  resources: ["nodes","nodes/metrics","services","endpoints","pods"]
  verbs: ["get","list","watch"]
- nonResourceURLs: ["/metrics"]
  verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: kubecost-prometheus-query
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: kubecost-prometheus-query
subjects:
- kind: ServiceAccount
  name: kubecost-prometheus
  namespace: kubecost
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: kubecost-cluster-monitoring-view
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-monitoring-view
subjects:
- kind: ServiceAccount
  name: kubecost-prometheus
  namespace: kubecost
```
```bash
oc apply -f kubecost-rbac.yaml
```

## Step 3: Operator Install
```yaml
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: kubecost-operatorgroup
  namespace: kubecost
spec: {}
---
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: kubecost
  namespace: kubecost
spec:
  channel: alpha
  name: kubecost
  source: community-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
```
```bash
oc apply -f kubecost-subscription.yaml
```

## Step 4: Wait for Operator
```bash
oc get csv -n kubecost -w
```

## Step 5: (Optional) Azure Secret
```bash
AZURE_SUBSCRIPTION_ID=xxx
AZURE_TENANT_ID=xxx
AZURE_CLIENT_ID=xxx
AZURE_CLIENT_SECRET=xxx
cat <<EOF | oc apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: cloud-integration
  namespace: kubecost
type: Opaque
stringData:
  cloud-integration.json: |
    {
      "azure": [
        {
          "azureSubscriptionID": "${AZURE_SUBSCRIPTION_ID}",
          "azureTenantID": "${AZURE_TENANT_ID}",
          "azureClientID": "${AZURE_CLIENT_ID}",
          "azureClientSecret": "${AZURE_CLIENT_SECRET}",
          "azureStorageSubscriptionID": "${AZURE_SUBSCRIPTION_ID}"
        }
      ]
    }
EOF
```

## Step 6: Deploy Instance (With Azure)
```yaml
apiVersion: charts.kubecost.com/v1alpha1
kind: CostAnalyzer
metadata:
  name: kubecost-instance
  namespace: kubecost
spec:
  prometheus:
    external:
      url: https://thanos-querier.openshift-monitoring.svc:9091
    queryServiceAccountName: kubecost-prometheus
  global:
    grafana:
      enabled: false
      proxy: false
  kubecostModel:
    warmCache: true
    warmSavingsCache: true
    etl: true
    cloudIntegrationSecret: cloud-integration
    costModel:
      cpuCost: "0.03"
      memoryCost: "0.004"
      storageClassPricing:
        default: "0.04"
        nfs: "0.02"
        local: "0.01"
  ingress:
    enabled: false
  networkCosts:
    enabled: false
  serviceMonitor:
    enabled: false
```

## Step 6B: Without Azure
Remove `cloudIntegrationSecret`.

## Step 7: Create Route
```yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: kubecost-ui
  namespace: kubecost
spec:
  to:
    kind: Service
    name: kubecost-instance-cost-analyzer
  port:
    targetPort: 9090
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
```

## Step 8: Verify
```bash
oc get pods -n kubecost
oc get route kubecost-ui -n kubecost -o jsonpath='{.spec.host}'
oc get deployments -n kubecost
```

## Troubleshooting
```bash
oc get events -n kubecost --sort-by='.lastTimestamp'
oc describe pod -n kubecost -l app=cost-analyzer
oc logs -n kubecost deployment/kubecost-instance-cost-analyzer -c cost-model --tail=50
oc get secret cloud-integration -n kubecost -o yaml
```

## Uninstall
```bash
oc delete costanalyzer kubecost-instance -n kubecost
oc delete subscription kubecost -n kubecost
oc delete csv -n kubecost -l operators.coreos.com/kubecost.kubecost=
oc delete namespace kubecost
oc delete clusterrolebinding kubecost-prometheus-query kubecost-cluster-monitoring-view
oc delete clusterrole kubecost-prometheus-query
```

## Azure Setup Reference
```bash
az ad sp create-for-rbac --name "kubecost-cost-reader" --role "Cost Management Reader" --scopes /subscriptions/YOUR_SUBSCRIPTION_ID
az account show --query id -o tsv
```

## Configuration Options
```yaml
cpuCost: "0.05"
memoryCost: "0.006"
storageClassPricing:
  default: "0.05"
  premium: "0.10"
  nfs: "0.02"
```

## Access UI
1. Get route host
2. Open https://<host>
3. Wait 10–15 minutes for data

## Notes
- Without cloud integration costs are estimates
- Exclude system namespaces from reports
