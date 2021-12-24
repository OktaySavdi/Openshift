### Kubernetes NFS Subdir External Provisioner

**NFS subdir external provisioner** is an automatic provisioner that use your _existing and already configured_ NFS server to support dynamic provisioning of Kubernetes Persistent Volumes via Persistent Volume Claims. Persistent volumes are provisioned as `${namespace}-${pvcName}-${pvName}`.

Note: This repository is migrated from https://github.com/kubernetes-incubator/external-storage/tree/master/nfs-client. As part of the migration:
- The container image name and repository has changed to `k8s.gcr.io/sig-storage` and `nfs-subdir-external-provisioner` respectively.
- To maintain backward compatibility with earlier deployment files, the naming of NFS Client Provisioner is retained as `nfs-client-provisioner` in the deployment YAMLs.
- One of the pending areas for development on this repository is to add automated e2e tests. If you would like to contribute, please raise an issue or reach us on the Kubernetes slack #sig-storage channel.

### How to deploy NFS Subdir External Provisioner to your cluster

To note again, you must _already_ have an NFS Server.

### With Helm

Follow the instructions from the helm chart [README](https://github.com/kubernetes-sigs/nfs-subdir-external-provisioner/blob/master/charts/nfs-subdir-external-provisioner/README.md).

The tl;dr is

```console
$ helm repo add nfs-subdir-external-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
$ helm install nfs-subdir-external-provisioner nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
    --set nfs.server=x.x.x.x \
    --set nfs.path=/exported/path
```

### Without Helm

**Step 1: Get connection information for your NFS server**

Make sure your NFS server is accessible from your Kubernetes cluster and get the information you need to connect to it. At a minimum you will need its hostname.

**Step 2: Get the NFS Subdir External Provisioner files**

To setup the provisioner you will download a set of YAML files, edit them to add your NFS server's connection information and then apply each with the `kubectl` / `oc` command.

Get all of the files in the [deploy](https://github.com/kubernetes-sigs/nfs-subdir-external-provisioner/tree/master/deploy) directory of this repository. These instructions assume that you have cloned the [kubernetes-sigs/nfs-subdir-external-provisioner](https://github.com/kubernetes-sigs/nfs-subdir-external-provisioner/) repository and have a bash-shell open in the root directory.

**Step 3: Setup authorization**

If your cluster has RBAC enabled or you are running OpenShift you must authorize the provisioner. If you are in a namespace/project other than "default" edit `deploy/rbac.yaml`.

**OpenShift:**

On some installations of OpenShift the default admin user does not have cluster-admin permissions. If these commands fail refer to the OpenShift documentation for **User and Role Management** or contact your OpenShift provider to help you grant the right permissions to your admin user.
On OpenShift the service account used to bind volumes does not have the necessary permissions required to use the `hostmount-anyuid` SCC. See also [Role based access to SCC](https://docs.openshift.com/container-platform/4.4/authentication/managing-security-context-constraints.html#role-based-access-to-ssc_configuring-internal-oauth) for more information. If these commands fail refer to the OpenShift documentation for **User and Role Management** or contact your OpenShift provider to help you grant the right permissions to your admin user.

```sh
# Set the subject of the RBAC objects to the current namespace where the provisioner is being deployed
NAMESPACE=`oc project -q`
sed -i'' "s/namespace:.*/namespace: $NAMESPACE/g" ./deploy/new-rbac.yaml ./deploy/deployment.yaml
oc create -f deploy/new-rbac.yaml
oc adm policy add-scc-to-user hostmount-anyuid system:serviceaccount:$NAMESPACE:nfs-client-provisioner
oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:nfs-dynamic-provider:nfs-client-provisioner
```

**Step 4: Configure the NFS subdir external provisioner**

If you would like to use a custom built nfs-subdir-external-provisioner image, you must edit the provisioner's deployment file to specify the correct location of your `nfs-client-provisioner` container image.

Next you must edit the provisioner's deployment file to add connection information for your NFS server. Edit `deploy/deployment.yaml` and replace the two occurences of <YOUR NFS SERVER HOSTNAME> with your server's hostname.

```yaml
kind: Deployment
apiVersion: apps/v1
metadata:
  name: nfs-client-provisioner
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nfs-client-provisioner
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: nfs-client-provisioner
    spec:
      serviceAccountName: nfs-client-provisioner
      containers:
        - name: nfs-client-provisioner
          image: k8s.gcr.io/sig-storage/nfs-subdir-external-provisioner:v4.0.2
          volumeMounts:
            - name: nfs-client-root
              mountPath: /persistentvolumes
          env:
            - name: PROVISIONER_NAME
              value: k8s-sigs.io/nfs-subdir-external-provisioner
            - name: NFS_SERVER
              value: <YOUR NFS SERVER HOSTNAME>
            - name: NFS_PATH
              value: </var/nfs>
      volumes:
        - name: nfs-client-root
          nfs:
            server: <YOUR NFS SERVER HOSTNAME>
            path: </var/nfs>
```

Note: If you want to change the PROVISIONER_NAME above from `k8s-sigs.io/nfs-subdir-external-provisioner` to something else like `myorg/nfs-storage`, remember to also change the PROVISIONER_NAME in the storage class definition below.

To disable leader election, define an env variable named ENABLE_LEADER_ELECTION and set its value to false.

**Step 5: Deploying your storage class**

**_Parameters:_**

| Name            | Description                                                                                                                                                                  |                             Default                              |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------: |
| onDelete        | If it exists and has a delete value, delete the directory, if it exists and has a retain value, save the directory.                                                          | will be archived with name on the share: `archived-<volume.Name>` |
| archiveOnDelete | If it exists and has a false value, delete the directory. if `onDelete` exists, `archiveOnDelete` will be ignored.                                                           | will be archived with name on the share: `archived-<volume.Name>` |
| pathPattern     | Specifies a template for creating a directory path via PVC metadata's such as labels, annotations, name or namespace. To specify metadata use `${.PVC.<metadata>}`. Example: If folder should be named like `<pvc-namespace>-<pvc-name>`, use `${.PVC.namespace}-${.PVC.name}` as pathPattern. |                               n/a                                |

This is `deploy/class.yaml` which defines the NFS subdir external provisioner's Kubernetes Storage Class:

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: managed-nfs-storage
provisioner: k8s-sigs.io/nfs-subdir-external-provisioner # or choose another name, must match deployment's env PROVISIONER_NAME'
parameters:
  pathPattern: "${.PVC.namespace}/${.PVC.annotations.nfs.io/storage-path}" # waits for nfs.io/storage-path annotation, if not specified will accept as empty string.
  onDelete: delete
```

**Step 6: Finally, test your environment!**

Now we'll test your NFS subdir external provisioner.

Deploy:

```sh
oc create -f deploy/test-claim.yaml -f deploy/test-pod.yaml
```

Now check your NFS Server for the file `SUCCESS`.

```sh
oc delete -f deploy/test-pod.yaml -f deploy/test-claim.yaml
```

Now check the folder has been deleted.

**Step 7: Deploying your own PersistentVolumeClaims**

To deploy your own PVC, make sure that you have the correct `storageClassName` as indicated by your `deploy/class.yaml` file.

For example:

```yaml
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: test-claim
  annotations:
    nfs.io/storage-path: "test-path" # not required, depending on whether this annotation was shown in the storage class description
spec:
  storageClassName: managed-nfs-storage
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 1Mi
```
