# Kdump Manual Installation - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Decide target node roles (master/worker/infra)

## Step 1: Create kdump.conf
```bash
cat > kdump.conf <<'EOF'
path /var/crash
core_collector makedumpfile -l --message-level 1 -d 31
disk_quota 2048
default reboot
EOF
```

## Step 2: Base64 Encode
```bash
KDUMP_CONF_BASE64=$(base64 -w 0 kdump.conf)
```

## Step 3: Master MachineConfig
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: master
  name: 99-master-kdump
spec:
  kernelArguments:
    - crashkernel=512M
  config:
    ignition:
      version: 3.2.0
    storage:
      files:
        - path: /etc/kdump.conf
          mode: 0644
          overwrite: true
          contents:
            source: data:text/plain;charset=utf-8;base64,${KDUMP_CONF_BASE64}
    systemd:
      units:
        - name: kdump.service
          enabled: true
```

## Step 4: Worker MachineConfig
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: worker
  name: 99-worker-kdump
spec:
  kernelArguments:
    - crashkernel=512M
  config:
    ignition:
      version: 3.2.0
    storage:
      files:
        - path: /etc/kdump.conf
          mode: 0644
          overwrite: true
          contents:
            source: data:text/plain;charset=utf-8;base64,${KDUMP_CONF_BASE64}
    systemd:
      units:
        - name: kdump.service
          enabled: true
```

## Step 5: Monitor Node Updates
```bash
oc get mcp -w
oc get mcp master -w
oc get mcp worker -w
```

## Verification
```bash
oc get mcp
NODE_NAME="your-node"
oc debug node/${NODE_NAME} -- chroot /host systemctl status kdump
oc debug node/${NODE_NAME} -- chroot /host cat /proc/cmdline | grep crashkernel
oc debug node/${NODE_NAME} -- chroot /host cat /etc/kdump.conf
oc debug node/${NODE_NAME} -- chroot /host ls -lh /var/crash/
```

## Troubleshooting
```bash
oc debug node/${NODE_NAME} -- chroot /host systemctl status kdump
oc debug node/${NODE_NAME} -- chroot /host journalctl -u kdump
oc get mcp master -o yaml
oc logs -n openshift-machine-config-operator -l k8s-app=machine-config-daemon --tail=100
```

## Uninstall
```bash
oc delete mc 99-master-kdump
oc delete mc 99-worker-kdump
oc get mcp -w
```

## Configuration Options
- Larger memory: `crashkernel=1024M`
- Custom path: modify `path` in kdump.conf
- Disk quota: adjust `disk_quota`

## Memory Recommendations
- <8GB RAM: 256M
- 8–32GB RAM: 512M
- >32GB RAM: 1024M

## Notes
- Nodes reboot applying MachineConfig
- Dumps compressed
- Automatic cleanup when quota reached
