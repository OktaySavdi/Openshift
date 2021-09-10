### Associating scc with serviceaccount

#Creating New Security Context Constraints

```yaml
cat <<EOF | oc create -f -
allowHostDirVolumePlugin: false
allowHostIPC: false
allowHostNetwork: false
allowHostPID: false
allowHostPorts: false
allowPrivilegeEscalation: true
allowPrivilegedContainer: false
allowedCapabilities: null
apiVersion: security.openshift.io/v1
defaultAddCapabilities: null
fsGroup:
  type: MustRunAs
groups:
- system:authenticated
kind: SecurityContextConstraints
metadata:
  name: nfs
priority: null
readOnlyRootFilesystem: false
requiredDropCapabilities:
- KILL
- MKNOD
- SETUID
- SETGID
runAsUser:
  type: MustRunAs
  uid: 65534
seLinuxContext:
  type: MustRunAs
supplementalGroups:
  type: RunAsAny
users: []
volumes:
- configMap
- downwardAPI
- emptyDir
- persistentVolumeClaim
- projected
- secret
EOF
```

#Grant a Service Account Access to the Privileged SCC
```yaml
cat <<EOF | oc create -f -
---
kind: ServiceAccount
apiVersion: v1
metadata:
  name: mysa
EOF
```

#Implementing RBAC
```sh
oc adm policy add-scc-to-user nfs -z mysa
```
or
```yaml
cat <<EOF | oc create -f -
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: allow-nfs-scc
rules:
- apiGroups:
    - security.openshift.io
  resourceNames:
    - nfs
  resources:
    - securitycontextconstraints
  verbs:
    - use
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: allow-nfs-scc
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: allow-nfs-scc
subjects:
- kind: ServiceAccount
  name: mysa
  namespace: default
---
EOF
```

#Create a sample application that starts up a basic image using the my-special-pod service account which should enable the pod to run as the designated user ID.
```sh
oc run my-special-pod --image=quay.io/oktaysavdi/istioproject --serviceaccount=batch-scc-sa --command -- /bin/bash -c 'while true; do sleep 3; done'
```
#check which policy it received
```sh
oc get pod my-special-pod -o jsonpath="{.metadata.annotations['openshift\.io/scc']}"
```
