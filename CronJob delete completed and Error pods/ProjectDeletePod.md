```yaml
kind: ServiceAccount
apiVersion: v1
metadata:
  name: clear-job
  namespace: oktay
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: clear-completed-pods-role
  namespace: oktay
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "watch", "list", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: clear-pods-rb
  namespace: oktay
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: clear-completed-pods-role
subjects:
- kind: ServiceAccount
  name: clear-job
  namespace: oktay
---
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: clear-completed-job
  namespace: oktay
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
            command: ["sh", "-c", "oc get po | grep -E \"Completed|Error\" | awk '{print $1}' | xargs oc delete pod  --grace-period=0 --force"]
          restartPolicy: OnFailure
  schedule: '0 0 * * *'
```
