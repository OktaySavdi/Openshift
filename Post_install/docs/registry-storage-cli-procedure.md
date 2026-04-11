# Registry Storage Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Storage backend ready (NFS, PVC, Object)
- Image registry operator installed

## Overview
Configure persistent storage backends for internal image registry.

## Step 1: Inspect Current Registry
```bash
oc get config.imageregistry.operator.openshift.io/cluster -o yaml
oc get pods -n openshift-image-registry
oc get config.imageregistry.operator.openshift.io/cluster -o jsonpath='{.spec.storage}'
```

## Step 2: PVC Storage
Automatic:
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"storage":{"pvc":{"claim":""}}}}'
```
Existing PVC:
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: image-registry-storage
  namespace: openshift-image-registry
spec:
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 100Gi
  storageClassName: nfs-storage
```
```bash
oc apply -f registry-pvc.yaml
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"storage":{"pvc":{"claim":"image-registry-storage"}}}}'
```

## Step 3: NFS Storage
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: registry-nfs-pv
spec:
  capacity:
    storage: 100Gi
  accessModes:
  - ReadWriteMany
  nfs:
    path: /exports/registry
    server: nfs.example.com
  persistentVolumeReclaimPolicy: Retain
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: image-registry-storage
  namespace: openshift-image-registry
spec:
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 100Gi
  volumeName: registry-nfs-pv
```
```bash
oc apply -f nfs-registry.yaml
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"storage":{"pvc":{"claim":"image-registry-storage"}}}}'
```

## Step 4: S3 Storage
```bash
oc create secret generic image-registry-private-configuration-user \
  --from-literal=REGISTRY_STORAGE_S3_ACCESSKEY=<access-key> \
  --from-literal=REGISTRY_STORAGE_S3_SECRETKEY=<secret-key> \
  -n openshift-image-registry
```
```yaml
apiVersion: imageregistry.operator.openshift.io/v1
kind: Config
metadata:
  name: cluster
spec:
  storage:
    s3:
      bucket: openshift-registry-bucket
      region: us-east-1
      regionEndpoint: https://s3.amazonaws.com
      encrypt: true
  managementState: Managed
```

## Step 5: Replicas
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"replicas":2}}'
# Single replica
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"replicas":1}}'
```

## Step 6: Default Route
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"defaultRoute":true}}'
oc get route default-route -n openshift-image-registry
```

## Step 7: Node Placement
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"nodeSelector":{"node-role.kubernetes.io/infra":""},"tolerations":[{"key":"node-role.kubernetes.io/infra","effect":"NoSchedule","operator":"Exists"}]}}'
```

## Verification
```bash
oc get config.imageregistry.operator.openshift.io/cluster -o yaml
oc get pods -n openshift-image-registry
oc get pvc -n openshift-image-registry
oc get route -n openshift-image-registry
```

## Troubleshooting
```bash
oc get events -n openshift-image-registry
oc describe pod -n openshift-image-registry <pod>
oc get pvc -n openshift-image-registry
oc describe pvc image-registry-storage -n openshift-image-registry
oc logs -n openshift-image-registry deployment/image-registry
oc get co image-registry
```

## Remove Storage Config
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"storage":{"emptyDir":{}}}}'
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"managementState":"Removed"}}'
oc delete pvc image-registry-storage -n openshift-image-registry
```

## Resize PVC
```bash
oc patch pvc image-registry-storage -n openshift-image-registry -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'
```

## Resource Limits
```bash
oc patch config.imageregistry.operator.openshift.io/cluster --type merge -p '{"spec":{"resources":{"requests":{"cpu":"100m","memory":"256Mi"},"limits":{"cpu":"500m","memory":"512Mi"}}}}'
```

## Backend Comparison
- NFS: Simple, RWX, lower performance
- S3/Object: Durable, scalable
- PVC RWX: Common production choice
- emptyDir: Non-persistent testing only

## Best Practices
- Use RWX for multi-replica
- Size >=100Gi production
- Enable pruning to manage growth
- Place on infra nodes
- Monitor usage & resize proactively

## Notes
- Storage changes restart pods
- Object storage recommended in cloud
- Monitor PVC capacity regularly
