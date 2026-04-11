# Image Pruning Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Internal image registry running
- Adequate disk space

## Overview
Automate pruning of unused images to reclaim registry storage.

## Step 1: Enable Automatic Pruning
```yaml
apiVersion: imageregistry.operator.openshift.io/v1
kind: ImagePruner
metadata:
  name: cluster
spec:
  schedule: "0 0 * * *"
  suspend: false
  keepTagRevisions: 3
  keepYoungerThan: 60m
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
  logLevel: Normal
```
```bash
oc apply -f imagepruner.yaml
```

## Step 2: Adjust Parameters
```bash
oc patch imagepruner.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"schedule":"0 2 * * 0","keepTagRevisions":5,"keepYoungerThan":168h}}'
```
Schedules examples:
- Daily: `0 0 * * *`
- Weekly: `0 2 * * 0`
- Monthly: `0 3 1 * *`
- Every 6h: `0 */6 * * *`

## Step 3: Manual Pruning
```bash
oc adm prune images                # dry-run
oc adm prune images --confirm
oc adm prune images --keep-tag-revisions=3 --keep-younger-than=60m --confirm
oc adm prune images --registry-url=image-registry.openshift-image-registry.svc:5000 --confirm
```

## Step 4: Prune Deployments
```bash
oc adm prune deployments --confirm
oc adm prune deployments --keep-complete=5 --keep-failed=1 --confirm
```

## Step 5: Prune Builds
```bash
oc adm prune builds --confirm
oc adm prune builds --keep-complete=5 --keep-failed=1 --confirm
oc adm prune builds --orphans --confirm
```

## Verification
```bash
oc get imagepruner.imageregistry.operator.openshift.io/cluster -o yaml
oc get cronjob -n openshift-image-registry
oc get jobs -n openshift-image-registry
oc logs -n openshift-image-registry job/image-pruner-<timestamp>
oc exec -n openshift-image-registry deployment/image-registry -- df -h /registry
```

## Troubleshooting
```bash
oc get imagepruner cluster -o yaml | grep -A10 status
oc describe cronjob -n openshift-image-registry
oc create job --from=cronjob/image-pruner manual-prune -n openshift-image-registry
oc logs -n openshift-image-registry -l job-name=manual-prune
oc get config.imageregistry.operator.openshift.io/cluster -o yaml
oc get pods -n openshift-image-registry
oc adm prune images --all --confirm  # force
```

## Disable Automatic Pruning
```bash
oc patch imagepruner.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"suspend":true}}'
oc delete imagepruner.imageregistry.operator.openshift.io/cluster
```

## Configuration Options
Production:
```yaml
keepTagRevisions: 5
keepYoungerThan: 168h
schedule: "0 2 * * 0"
```
Dev (aggressive):
```yaml
keepTagRevisions: 1
keepYoungerThan: 24h
schedule: "0 */12 * * *"
```
Resources & tolerations example:
```yaml
spec:
  resources:
    requests:
      cpu: 100m
      memory: 256Mi
    limits:
      cpu: 500m
      memory: 512Mi
  tolerations:
  - key: node-role.kubernetes.io/infra
    effect: NoSchedule
    operator: Exists
  nodeSelector:
    node-role.kubernetes.io/infra: ""
```

## Manual Cleanup Examples
```bash
oc adm prune images --keep-tag-revisions=3
oc adm prune images --keep-younger-than=168h --confirm
oc adm prune builds --orphans=false --keep-complete=3 --confirm
oc adm prune builds --keep-failed=0 --confirm
```

## Best Practices
- Start conservative
- Schedule low-traffic hours
- Keep at least 3 tag revisions
- Use dry-run before production prune
- Combine with storage monitoring

## Notes
- In-use images never pruned
- Hard prune frees blobs
- CronJob manages pruning jobs
- Large registries take longer
