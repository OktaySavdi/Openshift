# REMOVE WORKER LABEL FROM MASTERS - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- Compact cluster (3 masters also running workloads) or standard cluster
- Understanding of scheduling implications

## OVERVIEW
Remove the worker role label from master nodes (in non-compact clusters) to prevent user workloads from scheduling on control plane nodes.

## STEP 1: CHECK CURRENT NODE LABELS
```bash
oc get nodes --show-labels
oc get nodes -l node-role.kubernetes.io/master --show-labels
oc get nodes -l node-role.kubernetes.io/worker
```

## STEP 2: REMOVE WORKER LABEL FROM MASTERS
```bash
oc label node master-0 node-role.kubernetes.io/worker-
oc label nodes -l node-role.kubernetes.io/master node-role.kubernetes.io/worker-
oc get nodes -l node-role.kubernetes.io/master --show-labels
```

## STEP 3: ADD TAINT TO PREVENT WORKLOADS (OPTIONAL)
```bash
oc adm taint nodes -l node-role.kubernetes.io/master node-role.kubernetes.io/master=:NoSchedule
```

## STEP 4: VERIFY POD SCHEDULING
```bash
oc get pods --all-namespaces -o wide --field-selector spec.nodeName=master-0
```
Only control plane/system pods should appear.

## STEP 5: RESCHEDULE EXISTING WORKLOADS (IF NEEDED)
```bash
oc get pods --all-namespaces -o wide | grep master
# Delete specific pods to reschedule
oc delete pod <pod-name> -n <namespace>
# Drain / uncordon (graceful)
oc adm drain master-0 --ignore-daemonsets --delete-emptydir-data
oc adm uncordon master-0
```

## VERIFICATION
```bash
oc get nodes
oc describe node master-0 | grep Taints
oc get pods --all-namespaces -o wide --field-selector spec.nodeName=master-0 | grep -v openshift | grep -v kube-system
```

## TROUBLESHOOTING
```bash
# Label not removed
oc get nodes master-0 -o yaml | grep labels -A10
oc edit node master-0  # remove worker label manually
# Pods stuck
oc describe pod <pod> -n <ns>
# No worker nodes available -> cannot remove label until workers exist
```

## RE-ADD WORKER LABEL (REVERT)
```bash
oc label nodes -l node-role.kubernetes.io/master node-role.kubernetes.io/worker=""
oc adm taint nodes -l node-role.kubernetes.io/master node-role.kubernetes.io/master:NoSchedule-
oc get nodes --show-labels
```

## COMPACT VS STANDARD CLUSTER
- Compact: 3 masters also act as workers; keep worker label for flexibility.
- Standard: Dedicated workers present; remove worker label for isolation.

## WHEN TO REMOVE WORKER LABEL
Remove if:
- Dedicated worker nodes exist
- Need production isolation
- Compliance / security requirements
Keep if:
- Compact cluster
- Dev/test environment
- Resource constraints

## CONFIGURATION OPTIONS (ALLOW SELECT PODS ON MASTERS)
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: master-permitted-pod
spec:
  tolerations:
  - key: node-role.kubernetes.io/master
    effect: NoSchedule
    operator: Exists
  nodeSelector:
    node-role.kubernetes.io/master: ""
```
```yaml
# Node affinity (avoid workers)
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: node-role.kubernetes.io/worker
            operator: Exists
```

## INFRASTRUCTURE WORKLOADS ON MASTERS
System/infra components with tolerations still run on masters (router, registry, monitoring, logging). Verify:
```bash
oc get pods -n openshift-ingress -o wide
oc get pods -n openshift-image-registry -o wide
oc get pods -n openshift-monitoring -o wide
```

## BEST PRACTICES
- Use dedicated workers in production
- Remove worker label when workers exist
- Apply master taints for strict isolation
- Monitor control plane resource usage
- Document cluster topology
- Avoid user workloads on masters

## NOTES
- Label changes are immediate
- Existing pods unaffected until rescheduled
- System pods tolerate master taints
- DaemonSets may still deploy to masters
- Compact clusters keep worker label for flexibility
