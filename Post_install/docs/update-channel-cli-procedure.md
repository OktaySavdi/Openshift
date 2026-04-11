# UPDATE CHANNEL CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- Valid Red Hat subscription
- Network connectivity to Red Hat update services

## OVERVIEW
Configure OpenShift cluster update channel to control which version stream receives updates from. This procedure focuses on setting the stable channel for production clusters.

## STEP 1: CHECK CURRENT CHANNEL
```bash
# View current cluster version and channel
oc get clusterversion

# Get detailed cluster version info
oc get clusterversion version -o yaml

# Show current channel only
oc get clusterversion version -o jsonpath='{.spec.channel}{"\n"}'

# Show current version and available updates
oc adm upgrade
```

## STEP 2: UNDERSTAND CHANNEL OPTIONS
```bash
# Available channels:
# - stable-4.x    : Production-ready releases (RECOMMENDED)
# - fast-4.x      : Early access to upcoming stable releases
# - eus-4.x       : Extended Update Support (4.14, 4.16, etc.)
# - candidate-4.x : Pre-release versions for testing

# View available channels for your cluster
oc get clusterversion version -o jsonpath='{.status.desired.channels[*]}{"\n"}'
```

## STEP 3: SET STABLE UPDATE CHANNEL
```bash
# Set to stable-4.19 channel
oc patch clusterversion/version --type merge \
  -p '{"spec":{"channel":"stable-4.19"}}'

# Verify channel was set
oc get clusterversion version -o jsonpath='{.spec.channel}{"\n"}'

# Alternative: Using oc adm upgrade
oc adm upgrade channel stable-4.19
```

## STEP 4: CONFIGURE UPSTREAM UPDATE SERVICE
```bash
# Set official Red Hat update service (default)
oc patch clusterversion/version --type merge \
  -p '{"spec":{"upstream":"https://api.openshift.com/api/upgrades_info/v1/graph"}}'

# Verify upstream configuration
oc get clusterversion version -o jsonpath='{.spec.upstream}{"\n"}'
```

## STEP 5: VIEW AVAILABLE UPDATES
```bash
# After channel change, check available updates
oc adm upgrade

# Include conditional/not-recommended updates
oc adm upgrade --include-not-recommended

# View update risks and conditions
oc get clusterversion version -o jsonpath='{.status.conditionalUpdates}{"\n"}' | jq
```

## VERIFICATION
```bash
# Verify complete configuration
oc get clusterversion version -o yaml | grep -A 5 "spec:"

# Expected output:
# spec:
#   channel: stable-4.19
#   upstream: https://api.openshift.com/api/upgrades_info/v1/graph

# Check cluster operators are healthy
oc get clusteroperators

# All operators should show: True, False, False (AVAILABLE, PROGRESSING, DEGRADED)
```

## ANSIBLE AUTOMATION
```bash
# Run the Ansible playbook to configure update channel
cd /path/to/Manage_Cluster
ansible-playbook playbooks/26-update-channel-config.yml

# Variables configured in vars/all_vars.yaml:
# enable_update_channel: true
# update_channel: "stable-4.19"
# update_service_upstream: "https://api.openshift.com/api/upgrades_info/v1/graph"
```

## TROUBLESHOOTING
```bash
# Channel not updating
oc describe clusterversion version

# Check cluster version operator logs
oc logs -n openshift-cluster-version -l k8s-app=cluster-version-operator --tail=50

# Verify network connectivity to update service
curl -I https://api.openshift.com/api/upgrades_info/v1/graph

# Reset channel if needed
oc patch clusterversion/version --type json \
  -p '[{"op":"remove","path":"/spec/channel"}]'
```

## PRODUCTION RECOMMENDATIONS
```bash
# For production clusters, use stable channel
oc patch clusterversion/version --type merge \
  -p '{"spec":{"channel":"stable-4.19"}}'

# For long-term support, use EUS channel
oc patch clusterversion/version --type merge \
  -p '{"spec":{"channel":"eus-4.14"}}'

# NEVER use candidate channel in production
```

## CHANGE CHANNEL (DIFFERENT VERSION)
```bash
# Change to different major/minor version channel
# Example: Moving from 4.14 to 4.19

# Check current version
oc get clusterversion version -o jsonpath='{.status.desired.version}{"\n"}'

# Set new channel
oc patch clusterversion/version --type merge \
  -p '{"spec":{"channel":"stable-4.19"}}'

# View upgrade path
oc adm upgrade
```

## NOTES
- Channel changes take effect immediately
- Changing channels does NOT automatically trigger upgrades
- After changing channels, review available updates with `oc adm upgrade`
- stable channel is recommended for production workloads
- EUS channels (4.14, 4.16) provide longer support windows
- fast channel receives updates before stable (for testing)
- Always backup etcd before major version upgrades

## RELATED PLAYBOOKS
- `09-cluster-upgrade-config.yml` - Configure maxUnavailable for rolling upgrades
- `26-update-channel-config.yml` - Automated update channel configuration

## REFERENCE
- Official Red Hat documentation: https://docs.openshift.com/container-platform/4.19/updating/understanding_updates/understanding-update-channels-release.html
