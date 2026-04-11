# Performance Tuning Manual Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Node role labeling complete

## Overview
Deploy Tuned profiles for infra (low latency) and workers (high throughput).

## Step 1: Infra Network-Latency Profile
```yaml
apiVersion: tuned.openshift.io/v1
kind: Tuned
metadata:
  name: infra-network-latency
  namespace: openshift-cluster-node-tuning-operator
spec:
  profile:
  - name: infra-network-latency
    data: |
      [main]
      summary=Tuned profile for infra nodes focusing on network latency
      include=openshift-node

      [sysctl]
      net.ipv4.tcp_fastopen=3
      net.core.somaxconn=4096
      net.ipv4.tcp_max_syn_backlog=8192
      net.netfilter.nf_conntrack_max=1048576
      net.netfilter.nf_conntrack_tcp_timeout_established=86400
      net.core.rmem_max=16777216
      net.core.wmem_max=16777216
      net.ipv4.tcp_rmem=4096 87380 16777216
      net.ipv4.tcp_wmem=4096 65536 16777216
      net.ipv4.tcp_congestion_control=bbr
      net.core.default_qdisc=fq

      [vm]
      vm.swappiness=10
  recommend:
  - priority: 20
    match:
    - label: node-role.kubernetes.io/infra
    profile: infra-network-latency
```

## Step 2: Worker Throughput Profile
```yaml
apiVersion: tuned.openshift.io/v1
kind: Tuned
metadata:
  name: worker-throughput
  namespace: openshift-cluster-node-tuning-operator
spec:
  profile:
  - name: worker-throughput
    data: |
      [main]
      summary=Tuned profile for worker nodes focusing on throughput
      include=openshift-node

      [sysctl]
      net.core.rmem_max=134217728
      net.core.wmem_max=134217728
      net.ipv4.tcp_rmem=4096 87380 67108864
      net.ipv4.tcp_wmem=4096 65536 67108864
      net.netfilter.nf_conntrack_max=524288
      fs.file-max=2097152

      [vm]
      vm.swappiness=5
      vm.dirty_ratio=20
      vm.dirty_background_ratio=5
      vm.vfs_cache_pressure=50
  recommend:
  - priority: 20
    match:
    - label: node-role.kubernetes.io/worker
      type: "!infra"
    profile: worker-throughput
```

## Step 3: Verify Profiles
```bash
oc get tuned -n openshift-cluster-node-tuning-operator
oc get tuned infra-network-latency -n openshift-cluster-node-tuning-operator -o yaml
```

## Step 4: Wait for Application
Takes ~30–60s, no reboots.

## Step 5: Verify Application
```bash
oc get profile -n openshift-cluster-node-tuning-operator
NODE_NAME=your-node
oc get profile -n openshift-cluster-node-tuning-operator ${NODE_NAME} -o yaml
oc debug node/${NODE_NAME} -- chroot /host tuned-adm active
oc debug node/${NODE_NAME} -- chroot /host sysctl net.netfilter.nf_conntrack_max
```

## Troubleshooting
```bash
oc get profile -n openshift-cluster-node-tuning-operator <node> -o yaml
oc logs -n openshift-cluster-node-tuning-operator -l app=tuned --tail=100
oc logs -n openshift-cluster-node-tuning-operator -l name=cluster-node-tuning-operator
```

## Uninstall
```bash
oc delete tuned infra-network-latency -n openshift-cluster-node-tuning-operator
oc delete tuned worker-throughput -n openshift-cluster-node-tuning-operator
```

## Testing
```bash
oc run test-pod --image=nicolaka/netshoot -it --rm -- ping -c 10 <service-ip>
oc debug node/${NODE_NAME} -- chroot /host cat /proc/sys/net/netfilter/nf_conntrack_count
```

## Configuration Options
- Adjust `nf_conntrack_max`
- Switch congestion control: `net.ipv4.tcp_congestion_control=cubic`
- Increase buffers: `net.core.rmem_max=268435456`

## Best Practices
- Test in staging
- Monitor metrics pre/post
- Apply only where benefit expected

## Expected Results
Infra: lower latency, improved HAProxy performance.
Workers: better sustained throughput, improved I/O.

## Notes
- Dynamic, no reboot
- Label-driven targeting
