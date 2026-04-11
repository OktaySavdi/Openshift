# CHRONY TIME SYNCHRONIZATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- NTP/Chrony server accessible from cluster
- butane CLI tool (for creating MachineConfig)

## OVERVIEW
Configure Chrony NTP client on OpenShift nodes using MachineConfig for accurate time synchronization.

## STEP 1: CREATE CHRONY CONFIG FILE
```bash
cat > chrony.conf <<EOF
# Use corporate NTP servers
server ntp1.example.com iburst
server ntp2.example.com iburst
server ntp3.example.com iburst

# Allow NTP client access from local network
allow 10.0.0.0/8

# Serve time even if not synchronized
local stratum 10

# Record clock drift
driftfile /var/lib/chrony/drift

# Enable kernel RTC synchronization
rtcsync

# Log measurements
logdir /var/log/chrony
EOF
```

## STEP 2: BASE64 ENCODE CONFIG
```bash
CHRONY_CONF=$(base64 -w0 chrony.conf)
echo $CHRONY_CONF
```

## STEP 3: CREATE MACHINECONFIG FOR WORKERS
```yaml
cat <<EOF | oc apply -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: worker
  name: 50-worker-chrony
spec:
  config:
    ignition:
      version: 3.2.0
    storage:
      files:
      - contents:
          source: data:text/plain;charset=utf-8;base64,${CHRONY_CONF}
        mode: 0644
        overwrite: true
        path: /etc/chrony.conf
EOF
```

## STEP 4: CREATE MACHINECONFIG FOR MASTERS
```yaml
cat <<EOF | oc apply -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: master
  name: 50-master-chrony
spec:
  config:
    ignition:
      version: 3.2.0
    storage:
      files:
      - contents:
          source: data:text/plain;charset=utf-8;base64,${CHRONY_CONF}
        mode: 0644
        overwrite: true
        path: /etc/chrony.conf
EOF
```

## ALTERNATIVE: USING BUTANE
```bash
# Create butane config
cat > worker-chrony.bu <<EOF
variant: openshift
version: 4.14.0
metadata:
  name: 50-worker-chrony
  labels:
    machineconfiguration.openshift.io/role: worker
storage:
  files:
  - path: /etc/chrony.conf
    mode: 0644
    overwrite: true
    contents:
      inline: |
        server ntp1.example.com iburst
        server ntp2.example.com iburst
        server ntp3.example.com iburst
        driftfile /var/lib/chrony/drift
        rtcsync
        logdir /var/log/chrony
EOF

# Convert to MachineConfig
butane worker-chrony.bu -o 50-worker-chrony.yaml

# Apply
oc apply -f 50-worker-chrony.yaml
```

## VERIFICATION
```bash
# Check MachineConfig created
oc get machineconfig | grep chrony

# Check MachineConfigPool status
oc get mcp

# Wait for nodes to update
oc get mcp -w

# Check node status
oc get nodes

# Debug on node
oc debug node/worker-0

# Inside debug pod
chroot /host

# Check chrony status
systemctl status chronyd

# Check time sources
chronyc sources -v

# Check tracking
chronyc tracking

# Check if time is synced
timedatectl status
```

## TROUBLESHOOTING
```bash
# MachineConfigPool degraded
oc get mcp -o yaml | grep -A10 degraded

# Check render logs
oc logs -n openshift-machine-config-operator <pod-name>

# Manually restart chronyd on node
oc debug node/worker-0
chroot /host
systemctl restart chronyd
systemctl status chronyd

# Test NTP connectivity
chronyc -n sources

# Check NTP server reachability
ping ntp1.example.com
```

## DELETE CHRONY CONFIG
```bash
# Remove MachineConfig
oc delete machineconfig 50-worker-chrony
oc delete machineconfig 50-master-chrony

# Wait for nodes to revert
oc get mcp -w
```

## CONFIGURATION OPTIONS
```bash
# Multiple NTP servers
server ntp1.example.com iburst
server ntp2.example.com iburst
pool pool.ntp.org iburst maxsources 4

# Prefer specific server
server ntp1.example.com iburst prefer

# Local reference clock
refclock PHC /dev/ptp0 poll 3 dpoll -2 offset 0

# Allow specific network
allow 192.168.1.0/24

# Maximum clock adjustment
maxupdateskew 100.0

# Step clock on startup if offset > 1 second
makestep 1.0 3

# Log changes
log measurements statistics tracking
```

## BEST PRACTICES
- Use at least 3 NTP servers
- Use corporate NTP servers if available
- Enable iburst for faster sync on startup
- Monitor time drift regularly
- Test before applying to production
- Apply to test MachineConfigPool first
- MachineConfig changes cause node reboots
- Plan maintenance window

## NOTES
- Changes trigger rolling reboot of nodes
- One node reboots at a time (MCP handles this)
- Master nodes update first, then workers
- Update takes 10-30 minutes per node
- Original config backed up by MCO
- Use butane for easier config management
