```yaml
kind: ServiceAccount
apiVersion: v1
metadata:
  name: clear-job
  namespace: default
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: clear-failed-pods-role
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "watch", "list", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: clearpods-rb
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: clear-failed-pods-role
subjects:
- kind: ServiceAccount
  name: clear-job
  namespace: default
---
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  creationTimestamp: null
  name: clear-failed-pods-job
  namespace: default
spec:
  jobTemplate:
    metadata:
      name: clear-completed-job
    spec:
      template:
        metadata:
          creationTimestamp: null
        spec:
          serviceAccountName: clear-job
          containers:
          - image: "registry.redhat.io/openshift4/ose-cli:v4.7"
            name: my-job
            command: ["sh", "-c", "oc get pod -A --no-headers | grep -v openshift | awk '{if ($4==\"Error\" || $4==\"Completed\") print \"oc delete pod \" $2 \" -n \" $1;}' | sh"]
          restartPolicy: OnFailure
  schedule: '0 0 * * *'
```
