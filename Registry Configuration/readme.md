
### Openshift Registry Configuration

**Collect the registry manage status**
```bash
oc get configs.imageregistry.operator.openshift.io cluster -o=jsonpath='{.spec.managementState}'
```
**Set the registry to Manage state**
```bash
oc patch configs.imageregistry.operator.openshift.io cluster --type merge --patch '{"spec":{"managementState":"Managed"}}'
```
**Check the existing PVCs in the image-registry project**
```bash
oc get pvc -n openshift-image-registry -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the registry PV if no PVC was found in the image-registry project**
```yaml
cat <<EOF | oc create -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: registry-myocpcluster-pvc
  namespace: openshift-image-registry
spec:
  accessModes:
    - ReadWriteMany
  volumeMode: Filesystem
  resources:
    requests:
      storage: 150Gi
EOF
```
```yaml
cat <<EOF | oc create -f -
apiVersion: v1
kind: PersistentVolume
metadata:
  name: registry-myocpcluster-pv
spec:
  capacity:
    storage: 150Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteMany
  nfs:
    path: /my_nfs/registry-myocpcluster
    server: my_nfs.mydomain.com.tr
EOF
```
**Check the current registry backend storage claim name**
```bash
oc get configs.imageregistry.operator.openshift.io cluster -o=jsonpath="{.spec.storage.pvc.claim}"
```
**Check the name of the newly created PVCs in the image-registry project if no current PVCs were found**
```bash
oc get pvc -n openshift-image-registry -o=jsonpath="{.items[*]['metadata.name']}"
```
**Update the registry backend storage if no PVCs were found**
```bash
oc patch configs.imageregistry.operator.openshift.io cluster --type=json -p '[{"op":"replace","path":"/spec/storage","value":{"pvc":{"claim":"registry-myocpcluster-pvc"}}}]'
```
**Check the registry default route status**
```bash
oc get configs.imageregistry.operator.openshift.io cluster -o=jsonpath="{.spec.defaultRoute}"
```
**Update the registry default route**
```bash
oc patch configs.imageregistry.operator.openshift.io/cluster --type=merge --patch '{"spec":{"defaultRoute":true}}'
```
