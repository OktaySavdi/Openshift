```yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    run: example
  name: example
spec:
  containers:
  - command:
    - sleep
    - "3600"
    image: registry.redhat.io/rhel7/rhel-tools@sha256:3f3c34c1faef64356447291313214ab89639876a49550aa0d3e474eca0e61234
    name: example
    resources: {}
  dnsPolicy: ClusterFirst
  restartPolicy: Always
status: {}
```
```
oc run example --image=registry.redhat.io/rhel7/rhel-tools@sha256:3f3c34c1faef64356447291313214ab89639876a49550aa0d3e474eca0e61234 --command -o yaml -- sleep 3600
```

URL  : https://catalog.redhat.com/software/containers/rhel7/rhel-tools/57ea8cf09c624c035f96f3bb?container-tabs=gti

Image: registry.redhat.io/rhel8/support-tools
