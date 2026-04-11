# Worker Kubelet Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Kubelet parameter understanding
- Maintenance window (node reboots)

## Overview
Tune worker kubelet settings via `KubeletConfig` CR for density, eviction, GC, logging.

## Step 1: View Current Config
```bash
oc get kubeletconfigs
oc debug node/worker-0 -- chroot /host cat /etc/kubernetes/kubelet.conf
systemctl status kubelet
journalctl -u kubelet
```

## Step 2: Base Worker KubeletConfig
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: KubeletConfig
metadata:
  name: worker-kubelet-config
spec:
  machineConfigPoolSelector:
    matchLabels:
      pools.operator.machineconfiguration.openshift.io/worker: ""
  kubeletConfig:
    podsPerCore: 10
    maxPods: 250
    systemReserved:
      cpu: 500m
      memory: 1Gi
    kubeReserved:
      cpu: 500m
      memory: 1Gi
    evictionHard:
      memory.available: "500Mi"
      nodefs.available: "10%"
      nodefs.inodesFree: "5%"
      imagefs.available: "15%"
    imageGCHighThresholdPercent: 85
    imageGCLowThresholdPercent: 80
    serializeImagePulls: false
    registryPullQPS: 10
    registryBurst: 20
```

## Step 3: Garbage Collection Focus
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: KubeletConfig
metadata:
  name: worker-gc-config
spec:
  machineConfigPoolSelector:
    matchLabels:
      pools.operator.machineconfiguration.openshift.io/worker: ""
  kubeletConfig:
    imageMinimumGCAge: 2m
    imageGCHighThresholdPercent: 85
    imageGCLowThresholdPercent: 80
    evictionSoft:
      memory.available: "1Gi"
      nodefs.available: "15%"
      imagefs.available: "20%"
    evictionSoftGracePeriod:
      memory.available: "1m30s"
      nodefs.available: "2m"
      imagefs.available: "2m"
    evictionHard:
      memory.available: "500Mi"
      nodefs.available: "10%"
      imagefs.available: "15%"
    evictionPressureTransitionPeriod: 30s
```

## Step 4: High-Density Workloads
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: KubeletConfig
metadata:
  name: high-density-kubelet
spec:
  machineConfigPoolSelector:
    matchLabels:
      pools.operator.machineconfiguration.openshift.io/worker: ""
  kubeletConfig:
    maxPods: 500
    podsPerCore: 0
    systemReserved:
      cpu: 1000m
      memory: 2Gi
    kubeReserved:
      cpu: 1000m
      memory: 2Gi
    serializeImagePulls: false
    registryPullQPS: 20
    registryBurst: 30
```

## Step 5: Log Rotation
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: KubeletConfig
metadata:
  name: worker-log-rotation
spec:
  machineConfigPoolSelector:
    matchLabels:
      pools.operator.machineconfiguration.openshift.io/worker: ""
  kubeletConfig:
    containerLogMaxSize: "50Mi"
    containerLogMaxFiles: 5
```

## Verification
```bash
oc get kubeletconfig
oc get mcp
oc get mcp -w
oc debug node/worker-0 -- chroot /host cat /etc/kubernetes/kubelet.conf
ps aux | grep kubelet
systemctl status kubelet
oc describe node worker-0 | grep -A10 'Capacity\|Allocatable'
```

## Troubleshooting
```bash
oc describe kubeletconfig worker-kubelet-config
oc get mcp worker -o yaml
oc describe mcp worker
oc logs -n openshift-machine-config-operator -l k8s-app=machine-config-daemon
oc debug node/worker-0 -- chroot /host journalctl -u machine-config-daemon
```

## Delete Config
```bash
oc delete kubeletconfig worker-kubelet-config
oc get mcp -w
```

## Additional Options
```yaml
kubeletConfig:
  cpuManagerPolicy: static
  topologyManagerPolicy: best-effort
  memoryManagerPolicy: Static
  evictionSoft:
    memory.available: "500Mi"
  systemReserved:
    cpu: "500m"
    memory: "1Gi"
```
Infra example:
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: KubeletConfig
metadata:
  name: infra-kubelet-config
spec:
  machineConfigPoolSelector:
    matchLabels:
      pools.operator.machineconfiguration.openshift.io/infra: ""
  kubeletConfig:
    maxPods: 100
    systemReserved:
      cpu: 2000m
      memory: 4Gi
    kubeReserved:
      cpu: 2000m
      memory: 4Gi
```

## Best Practices
- Test before production
- Plan for reboots
- Reserve adequate resources
- Monitor post-change

## Common Settings
Small (<16GB): maxPods 110
Medium (16–64GB): maxPods 250
Large (>64GB): maxPods 500

## Notes
- Rolling update reboots nodes
- Invalid config can degrade MCP
- Allocate system & kube reserved separately
