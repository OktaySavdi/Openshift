# Infrastructure Node Scheduling - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Infra nodes labeled or to be labeled

## Overview
Move cluster infrastructure workloads (router, registry, monitoring, logging) to dedicated infra nodes.

## Step 1: Label Infra Nodes
```bash
oc label node worker-0 node-role.kubernetes.io/infra=""
oc label nodes worker-0 worker-1 worker-2 node-role.kubernetes.io/infra=""
oc label node worker-0 node-role.kubernetes.io/worker-  # optional removal
```

## Step 2: Create MachineConfigPool
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfigPool
metadata:
  name: infra
spec:
  machineConfigSelector:
    matchExpressions:
    - key: machineconfiguration.openshift.io/role
      operator: In
      values:
      - worker
      - infra
  nodeSelector:
    matchLabels:
      node-role.kubernetes.io/infra: ""
  paused: false
```
```bash
oc apply -f infra-mcp.yaml
```

## Step 3: Move Ingress Controller
```bash
oc patch ingresscontroller/default -n openshift-ingress-operator --type merge -p '{"spec":{"nodePlacement":{"nodeSelector":{"matchLabels":{"node-role.kubernetes.io/infra":""}}}}}'
# Optionally add tolerations via edit
```

## Step 4: Move Registry
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"nodeSelector":{"node-role.kubernetes.io/infra":""}}}'
```

## Step 5: Move Monitoring
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    prometheusK8s:
      nodeSelector:
        node-role.kubernetes.io/infra: ""
      tolerations:
      - key: node-role.kubernetes.io/infra
        effect: NoSchedule
        operator: Exists
    alertmanagerMain:
      nodeSelector:
        node-role.kubernetes.io/infra: ""
      tolerations:
      - key: node-role.kubernetes.io/infra
        effect: NoSchedule
        operator: Exists
    grafana:
      nodeSelector:
        node-role.kubernetes.io/infra: ""
      tolerations:
      - key: node-role.kubernetes.io/infra
        effect: NoSchedule
        operator: Exists
    # ... (other components similarly)
```
```bash
oc apply -f monitoring-infra-config.yaml
```

## Step 6: Move Logging (If Installed)
Edit ClusterLogging CR adding nodeSelectors for `logStore`, `visualization`, `collection`.

## Step 7: Optional Taints
```bash
oc adm taint nodes -l node-role.kubernetes.io/infra node-role.kubernetes.io/infra=:NoSchedule
# Remove
oc adm taint nodes -l node-role.kubernetes.io/infra node-role.kubernetes.io/infra:NoSchedule-
```

## Verification
```bash
oc get nodes -l node-role.kubernetes.io/infra
oc get mcp infra
oc get pods -n openshift-ingress -o wide
oc get pods -n openshift-image-registry -o wide
oc get pods -n openshift-monitoring -o wide
oc get pods --all-namespaces -o wide | grep infra-node-name
```

## Troubleshooting
```bash
oc describe pod <pod> -n <ns>
oc describe node <infra-node> | grep -A5 Taints
oc delete pods -n openshift-ingress --all
oc get mcp infra -o yaml
```

## Cleanup
```bash
oc label nodes -l node-role.kubernetes.io/infra node-role.kubernetes.io/infra-
oc delete mcp infra
oc patch ingresscontroller/default -n openshift-ingress-operator --type json -p '[{"op":"remove","path":"/spec/nodePlacement"}]'
oc patch config.imageregistry.operator.openshift.io/cluster --type json -p '[{"op":"remove","path":"/spec/nodeSelector"}]'
oc delete configmap cluster-monitoring-config -n openshift-monitoring
```

## Best Practices
- Minimum 3 infra nodes for HA
- Use taints to isolate infra
- Monitor infra node resource usage
- Size nodes for router & monitoring needs

## Notes
- Infra nodes host cluster services
- Reduces contention with workloads
- Changes trigger pod relocations
