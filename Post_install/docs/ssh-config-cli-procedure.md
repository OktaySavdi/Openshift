# SSH Bastion Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- SSH key pair
- Bastion host (RHEL/CentOS preferred)

## Overview
Provide secure SSH access path to cluster nodes via bastion; configure authorized keys through MachineConfig.

## Step 1: Generate Keys
```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/openshift-bastion -C "openshift-bastion"
cat ~/.ssh/openshift-bastion.pub
```

## Step 2: Authorized Keys MachineConfig
Create file `authorized_keys` then encode if needed.
```bash
cat > authorized_keys <<EOF
ssh-rsa AAAAB3NzaC... admin@example.com
ssh-rsa AAAAB3NzaC... bastion@example.com
EOF
```
Worker:
```yaml
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  labels:
    machineconfiguration.openshift.io/role: worker
  name: 99-worker-ssh
spec:
  config:
    ignition:
      version: 3.2.0
    passwd:
      users:
      - name: core
        sshAuthorizedKeys:
        - ssh-rsa AAAAB3NzaC... admin@example.com
        - ssh-rsa AAAAB3NzaC... bastion@example.com
```
Master similar MC.

## Step 3: Bastion Host Setup
```bash
sudo dnf install -y openssh-server openssh-clients
sudo vi /etc/ssh/sshd_config
# Recommended:
# PermitRootLogin no
# PasswordAuthentication no
# PubkeyAuthentication yes
sudo systemctl restart sshd
sudo systemctl enable sshd
```

## Step 4: Firewall
```bash
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

## Step 5: SSH Config (Bastion)
```bash
cat > ~/.ssh/config <<EOF
Host master-*
  User core
  StrictHostKeyChecking no
  UserKnownHostsFile /dev/null
Host worker-*
  User core
  StrictHostKeyChecking no
  UserKnownHostsFile /dev/null
Host *.example.com
  User core
  IdentityFile ~/.ssh/openshift-bastion
EOF
chmod 600 ~/.ssh/config
```

## Step 6: Test Access
```bash
oc get nodes -o wide
ssh core@worker-0.example.com
```

## Verification
```bash
oc get machineconfig | grep ssh
oc get mcp
ssh core@master-0.example.com hostname
oc debug node/worker-0 -- chroot /host cat /home/core/.ssh/authorized_keys
```

## Troubleshooting
```bash
oc get machineconfig 99-worker-ssh -o yaml
oc get mcp
ping worker-0.example.com
oc debug node/worker-0 -- chroot /host systemctl status sshd
journalctl -u machine-config-daemon
```

## Delete Config
```bash
oc delete machineconfig 99-worker-ssh
oc delete machineconfig 99-master-ssh
oc get mcp -w
```

## Security Hardening
- Disable password auth
- Restrict source IPs via firewall rich rules
- Enable SELinux enforcing
- Consider Fail2ban

## Jump Host Usage
Workstation -> Bastion -> Nodes via `ProxyJump`.

## Automation (Ansible Example)
```ini
[bastion]
bastion.example.com ansible_user=admin
[masters]
master-0.example.com ansible_user=core
[workers]
worker-0.example.com ansible_user=core
[all:vars]
ansible_ssh_common_args='-o ProxyCommand="ssh -W %h:%p admin@bastion.example.com"'
```

## Notes
- MachineConfig triggers node reboot
- Prefer `oc debug` for routine ops
- Keep key rotation schedule
