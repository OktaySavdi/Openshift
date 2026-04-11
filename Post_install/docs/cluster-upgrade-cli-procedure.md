# Cluster Upgrade Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Connected cluster or mirrored registry
- Valid Red Hat subscription
- Recent cluster backup

## Overview
Manage OpenShift cluster upgrades using the `ClusterVersion` resource and `oc adm upgrade` commands.

## Step 1: Check Current Version
```bash
oc get clusterversion
oc get clusterversion version -o yaml
oc get clusterversion version -o jsonpath='{.spec.channel}'
```

## Step 2: View Available Updates
```bash
oc adm upgrade
oc adm upgrade --to=4.14.10
```

## Step 3: Set Update Channel
```bash
oc patch clusterversion/version --type merge -p '{"spec":{"channel":"stable-4.14"}}'
oc patch clusterversion/version --type merge -p '{"spec":{"channel":"fast-4.14"}}'
oc patch clusterversion/version --type merge -p '{"spec":{"channel":"eus-4.14"}}'
oc patch clusterversion/version --type merge -p '{"spec":{"channel":"candidate-4.14"}}'
```

## Step 4: Pause/Unpause Upgrades
```bash
# Pause
oc patch clusterversion/version --type merge -p '{"spec":{"overrides":[{"kind":"Deployment","name":"cluster-version-operator","namespace":"openshift-cluster-version","unmanaged":true}]}}'
# Unpause
oc patch clusterversion/version --type json -p '[{"op":"remove","path":"/spec/overrides"}]'
```

## Step 5: Start Upgrade
```bash
oc adm upgrade --to-latest=true
oc adm upgrade --to=4.14.10
oc adm upgrade --to=4.14.10 --force --allow-explicit-upgrade
```

## Step 6: Monitor Upgrade
```bash
oc get clusterversion -w
oc get clusteroperators
oc get clusterversion version -o jsonpath='{.status.conditions[?(@.type=="Progressing")]}'
oc get nodes
oc get mcp -w
```

## Verification
```bash
oc get clusterversion
oc get co
oc get nodes -o custom-columns=NAME:.metadata.name,VERSION:.status.nodeInfo.kubeletVersion
oc get alerts -n openshift-monitoring
```

## Rollback (Not Recommended)
```bash
oc get clusterversion version -o jsonpath='{.status.history}'
oc adm upgrade --to=<previous-version> --force --allow-explicit-upgrade
```

## Troubleshooting
```bash
oc get clusterversion version -o yaml | grep -A20 status
oc get co | grep -v "True.*False.*False"
oc logs -n openshift-cluster-version deployment/cluster-version-operator
oc get mcp
oc describe mcp worker
oc describe node <node-name>
oc adm upgrade --to=4.14.10 --include-not-recommended
```

## Upgrade Strategies
1. Automatic (not recommended production): set upstream + channel.
2. Manual (recommended): run `oc adm upgrade --to=<version>`.
3. EUS to EUS: upgrade latest z-stream then switch channel.
```bash
oc patch clusterversion/version --type merge -p '{"spec":{"channel":"eus-4.14"}}'
oc adm upgrade --to=4.14.z
oc patch clusterversion/version --type merge -p '{"spec":{"channel":"eus-4.16"}}'
oc adm upgrade --to=4.16.z
```

## Pre-Upgrade Checklist
```bash
# Backup etcd (example)
oc get pods -n openshift-etcd | grep etcd-
# Health
oc get co
oc get nodes
oc get mcp
# Alerts
oc get alerts -n openshift-monitoring
# Storage
oc get pv
oc get sc
# Network
oc get network.operator cluster -o yaml
```

## Configuration Options
```bash
# Disconnected upgrade
oc patch clusterversion/version --type merge -p '{"spec":{"upstream":"http://mirror.example.com/upgrades"}}'
# Signature store
oc patch clusterversion/version --type merge -p '{"spec":{"signatureStores":[{"url":"https://mirror.example.com/signatures"}]}}'
# Exclude an operator
oc patch clusterversion/version --type json -p '[{"op":"add","path":"/spec/overrides","value":[{"kind":"Deployment","name":"console-operator","namespace":"openshift-console","unmanaged":true}]}]'
```

## Best Practices
- Upgrade to latest z-stream first
- Test in non-production
- Backup before upgrade
- Review release notes
- Schedule maintenance window
- Monitor during upgrade
- Perform during low traffic
- One minor version at a time
- Use stable channel for production

## Notes
- Typical duration: 1–3 hours
- Nodes reboot
- Control plane first, then workers
- No full downgrade path
- EUS supported ~18 months
- Read compatibility matrix
