# COMPLIANCE OPERATOR - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- OpenShift 4.10 or higher

## OVERVIEW
Deploy and configure Compliance Operator for CIS and PCI-DSS security scanning.

## STEP 1: INSTALL COMPLIANCE OPERATOR

```bash
# Create namespace
oc create namespace openshift-compliance

# Create operator group
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: compliance-operator
  namespace: openshift-compliance
spec:
  targetNamespaces:
    - openshift-compliance
EOF

# Create subscription
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: compliance-operator
  namespace: openshift-compliance
spec:
  channel: stable
  installPlanApproval: Automatic
  name: compliance-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
EOF

# Verify installation
oc get csv -n openshift-compliance
oc get pods -n openshift-compliance
```

## STEP 2: WAIT FOR PROFILES

```bash
# Wait for profiles to be available (takes 2-5 minutes)
watch oc get profiles.compliance -n openshift-compliance

# List available profiles
oc get profiles.compliance -n openshift-compliance
```

## STEP 3: CONFIGURE CIS BENCHMARK SCAN

```bash
# Create scan setting
cat <<EOF | oc apply -f -
apiVersion: compliance.openshift.io/v1alpha1
kind: ScanSetting
metadata:
  name: cis-scan-setting
  namespace: openshift-compliance
spec:
  autoApplyRemediations: false
  autoUpdateRemediations: true
  schedule: "0 2 * * 0"
  roles:
    - worker
    - master
  scanTolerations:
    - effect: NoSchedule
      key: node-role.kubernetes.io/master
      operator: Exists
EOF

# Create CIS scan binding
cat <<EOF | oc apply -f -
apiVersion: compliance.openshift.io/v1alpha1
kind: ScanSettingBinding
metadata:
  name: cis-compliance
  namespace: openshift-compliance
profiles:
  - name: ocp4-cis-node
    kind: Profile
  - name: ocp4-cis
    kind: Profile
settingsRef:
  name: cis-scan-setting
  kind: ScanSetting
EOF
```

## STEP 4: MONITOR SCAN PROGRESS

```bash
# Watch scan status
watch oc get compliancescan -n openshift-compliance

# View detailed scan status
oc describe compliancescan <scan-name> -n openshift-compliance
```

## STEP 5: VIEW SCAN RESULTS

```bash
# List all compliance check results
oc get compliancecheckresult -n openshift-compliance

# View only failed checks
oc get compliancecheckresult -n openshift-compliance \
  -l compliance.openshift.io/check-status=FAIL

# View specific check result
oc describe compliancecheckresult <result-name> -n openshift-compliance

# Export results summary
oc get compliancecheckresult -n openshift-compliance \
  -o custom-columns=NAME:.metadata.name,STATUS:.status,SEVERITY:.severity
```

## STEP 6: APPLY REMEDIATIONS

```bash
# List available remediations
oc get complianceremediations -n openshift-compliance

# View remediation details
oc describe complianceremediation <remediation-name> -n openshift-compliance

# Apply specific remediation
oc patch complianceremediation <remediation-name> -n openshift-compliance \
  --type=merge -p '{"spec":{"apply":true}}'

# Apply all remediations (use with caution!)
oc get complianceremediations -n openshift-compliance -o name | \
  xargs -I {} oc patch {} --type=merge -p '{"spec":{"apply":true}}'
```

## STEP 7: EXPORT COMPLIANCE REPORT

```bash
# Install compliance CLI (if not already installed)
oc compliance -h || echo "Install compliance plugin"

# Fetch compliance report
oc compliance fetch-raw scansettingbindings cis-compliance -o /tmp/compliance-results/

# View HTML report (generated in output directory)
ls -lh /tmp/compliance-results/
```

## PCI-DSS SCANNING (OPTIONAL)

```bash
# Create PCI-DSS scan binding
cat <<EOF | oc apply -f -
apiVersion: compliance.openshift.io/v1alpha1
kind: ScanSettingBinding
metadata:
  name: pci-dss-compliance
  namespace: openshift-compliance
profiles:
  - name: ocp4-pci-dss-node
    kind: Profile
  - name: ocp4-pci-dss
    kind: Profile
settingsRef:
  name: cis-scan-setting
  kind: ScanSetting
EOF
```

## ANSIBLE AUTOMATION

```bash
# Run Compliance Operator playbook
cd /path/to/Manage_Cluster
ansible-playbook playbooks/27-compliance-operator.yml --vault-password-file=vault_pass

# Variables in vars/all_vars.yaml:
# enable_compliance_operator: true
# enable_cis_scanning: true
# enable_pci_dss_scanning: false
# cis_scan_schedule: "0 2 * * 0"
# compliance_auto_apply_remediations: false
```

## RE-RUN COMPLIANCE SCAN

```bash
# Trigger rescan for specific scan
oc annotate compliancescan/ocp4-cis -n openshift-compliance \
  compliance.openshift.io/rescan=""

# Trigger rescan for CIS node scan
oc annotate compliancescan/ocp4-cis-node -n openshift-compliance \
  compliance.openshift.io/rescan=""

# Trigger all scans at once
oc get compliancescan -n openshift-compliance -o name | \
  xargs -I {} oc annotate {} compliance.openshift.io/rescan=""

# Monitor scan progress
oc get compliancescan -n openshift-compliance -w

# Check detailed scan status
oc describe compliancescan/ocp4-cis -n openshift-compliance
```

## TROUBLESHOOTING

```bash
# Check operator logs
oc logs -n openshift-compliance -l name=compliance-operator

# Check scan pod logs
oc logs -n openshift-compliance -l compliance-scan=<scan-name>

# Restart scan (same as re-run above)
oc annotate compliancescan/<scan-name> -n openshift-compliance \
  compliance.openshift.io/rescan=""

# Delete and recreate scan
oc delete scansettingbinding cis-compliance -n openshift-compliance
# Reapply scan configuration
```

## USEFUL COMMANDS

```bash
# Count failed checks
oc get compliancecheckresult -n openshift-compliance \
  -l compliance.openshift.io/check-status=FAIL --no-headers | wc -l

# Get compliance score
total=$(oc get compliancecheckresult -n openshift-compliance --no-headers | wc -l)
passed=$(oc get compliancecheckresult -n openshift-compliance \
  -l compliance.openshift.io/check-status=PASS --no-headers | wc -l)
echo "Compliance Score: $passed/$total ($(echo "scale=2; $passed*100/$total" | bc)%)"

# Filter by severity
oc get compliancecheckresult -n openshift-compliance \
  -l compliance.openshift.io/check-severity=high \
  -l compliance.openshift.io/check-status=FAIL
```

## REFERENCE
- Official Documentation: https://docs.openshift.com/container-platform/latest/security/compliance_operator/compliance-operator-understanding.html
