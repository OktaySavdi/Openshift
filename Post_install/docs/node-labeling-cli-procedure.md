# NODE LABELING CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- oc CLI logged in

## OVERVIEW
Label baremetal nodes with custom labels for workload placement, node selection, and infrastructure organization.

## STEP 1: LIST NODES
```bash
oc get nodes
oc get nodes --show-labels
oc get nodes -L node-role.kubernetes.io/worker
```

## STEP 2: LABEL WORKER NODES
```bash
# Single node
oc label node worker-0 node-type=baremetal
# Multiple nodes
oc label nodes worker-0 worker-1 worker-2 node-type=baremetal
# All workers
oc label nodes -l node-role.kubernetes.io/worker node-type=baremetal
```

## STEP 3: LABEL BY ZONE/REGION
```bash
oc label node worker-0 topology.kubernetes.io/zone=zone-a
oc label node worker-1 topology.kubernetes.io/zone=zone-b
oc label node worker-2 topology.kubernetes.io/zone=zone-c
oc label nodes -l node-role.kubernetes.io/worker topology.kubernetes.io/region=us-east
```

## STEP 4: LABEL FOR WORKLOAD TYPES
```bash
oc label node worker-0 workload=database
oc label node worker-1 workload=web
oc label node worker-2 workload=application
```

## STEP 5: LABEL INFRA NODES
```bash
oc label node worker-0 node-role.kubernetes.io/infra=""
# Optional remove worker role
oc label node worker-0 node-role.kubernetes.io/worker-
```

## VERIFICATION
```bash
oc get node worker-0 --show-labels
oc get nodes -l node-type=baremetal
oc get nodes -l workload=database
oc get nodes -l node-role.kubernetes.io/infra
oc describe node worker-0
```

## REMOVE LABELS
```bash
oc label node worker-0 node-type-
oc label nodes -l node-type=baremetal node-type-
```

## COMMON LABELS
```text
node-role.kubernetes.io/master=""
node-role.kubernetes.io/worker=""
node-role.kubernetes.io/infra=""
topology.kubernetes.io/zone=zone-a
topology.kubernetes.io/region=us-east
node.kubernetes.io/instance-type=m5.xlarge
kubernetes.io/arch=amd64
kubernetes.io/os=linux
workload=database|web|compute
tier=frontend|backend
environment=production
storage=fast-ssd|slow-hdd
node.openshift.io/local-storage=true
k8s.ovn.org/egress-assignable=""
```

## USING LABELS FOR POD PLACEMENT
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  nodeSelector:
    workload: database
    node-type: baremetal
  containers:
  - name: app
    image: myapp:latest
```
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: workload
            operator: In
            values: [database, storage]
  containers:
  - name: app
    image: myapp:latest
```

## TROUBLESHOOTING
```bash
oc get node <node> --show-labels | grep <label-key>
oc get nodes
oc describe node <node>
oc get pods -o wide
```

## BULK OPERATIONS
```bash
for node in $(oc get nodes -l node-role.kubernetes.io/worker -o name); do oc label $node node-type=baremetal; done
for node in $(oc get nodes -o name | grep worker); do oc label $node workload=general; done
```

## NOTES
- Labels are key-value pairs
- Used by nodeSelector / affinity
- Some system labels are immutable
- Use meaningful lowercase names
- Max label length: 63 chars
- Changes are immediate (no reboot)
