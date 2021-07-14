
### Openshift Chrony Configuration

**Check the existence of the elasticsearch operator namespace**
```bash
oc get machineconfigs.machineconfiguration.openshift.io -o=jsonpath="{.items[*]['metadata.name']}"| sed 's/ /,/g'
```
**Create the backup directory if doesn't exist and a change is due**
```bash
mkdir backup/< mycluster_name >
```
**Take a backup from the existing customized machine config for both master and worker if a change is due**
```bash
oc get machineconfigs.machineconfiguration.openshift.io "99-masters-chrony-configuration" -o yaml > backup/< mycluster_name >/99-masters-chrony-configuration_$(date +"%d%b%y_%H%M")

oc get machineconfigs.machineconfiguration.openshift.io "99-workers-chrony-configuration >" -o yaml > backup/< mycluster_name >/99-workers-chrony-configuration_$(date +"%d%b%y_%H%M")
```
**Genereate the required machine configs for both master and worker if a change is due**
Create the contents of the `chrony.conf` file and encode it as base64. For example:
```
cat << EOF | base64
    pool 0.rhel.pool.ntp.org iburst 
    driftfile /var/lib/chrony/drift
    makestep 1.0 3
    rtcsync
    logdir /var/log/chrony
EOF
```
  Specify any valid, reachable time source, such as the one provided by your DHCP server. Alternately, you can specify any of the following NTP servers:  `1.rhel.pool.ntp.org`,  `2.rhel.pool.ntp.org`, or  `3.rhel.pool.ntp.org`.

**Example output**
```
ICAgIHNlcnZlciBjbG9jay5yZWRoYXQuY29tIGlidXJzdAogICAgZHJpZnRmaWxlIC92YXIvbGli
L2Nocm9ueS9kcmlmdAogICAgbWFrZXN0ZXAgMS4wIDMKICAgIHJ0Y3N5bmMKICAgIGxvZ2RpciAv
dmFyL2xvZy9jaHJvbnkK
```
Create the `MachineConfig` object file, replacing the base64 string with the one you just created. This example adds the file to `master` nodes. You can change it to `worker` or make an additional MachineConfig for the `worker` role. Create MachineConfig files for each type of machine that your cluster uses:
```yaml
cat <<EOF | oc create -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: master
  name: 99-masters-chrony-configuration
spec:
  config:
    ignition:
      version: 2.2.0
    storage:
      files:
      - contents:
          source: data:text/plain;charset=utf-8;base64,ICAgIHNlcnZlciBjbG9jay5yZWRoYXQuY29tIGlidXJzdAogICAgZHJpZnRmaWxlIC92YXIvbGliL2Nocm9ueS9kcmlmdAogICAgbWFrZXN0ZXAgMS4wIDMKICAgIHJ0Y3N5bmMKICAgIGxvZ2RpciAvdmFyL2xvZy9jaHJvbnkK
        filesystem: root
        mode: 420
        path: /etc/chrony.conf
EOF
```
```yaml
cat <<EOF | oc create -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: worker
  name: 99-workers-chrony-configuration
spec:
  config:
    ignition:
      version: 2.2.0
    storage:
      files:
      - contents:
          source: data:text/plain;charset=utf-8;base64,ICAgIHNlcnZlciBjbG9jay5yZWRoYXQuY29tIGlidXJzdAogICAgZHJpZnRmaWxlIC92YXIvbGliL2Nocm9ueS9kcmlmdAogICAgbWFrZXN0ZXAgMS4wIDMKICAgIHJ0Y3N5bmMKICAgIGxvZ2RpciAvdmFyL2xvZy9jaHJvbnkK
        filesystem: root
        mode: 420
        path: /etc/chrony.conf
EOF
```
**Check the exitence of the customized machine config pool for infra**
```bash
oc get machineconfigpool --field-selector=metadata.name=infra -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the customized machine config pool infra if a change is due**
```yaml
cat <<EOF | oc create -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfigPool
metadata:
  name: infra
spec:
  machineConfigSelector:
    matchExpressions:
      - {key: machineconfiguration.openshift.io/role, operator: In, values: [worker,infra]}
  nodeSelector:
    matchLabels:
      node-role.kubernetes.io/infra: ""
  paused: false
EOF
```
**Label the customized machine config pool worker**
```bash
oc label mcp worker role=worker
```

**Referance :** 

[+] https://docs.openshift.com/container-platform/4.4/installing/install_config/installing-customizing.html#installation-special-config-chrony_installing-customizing
