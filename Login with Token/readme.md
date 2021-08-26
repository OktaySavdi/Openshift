### Get Token from Service Account
```
TOKEN=$(oc -n default get secret $(oc -n default get sa mysa -o jsonpath='{.secrets[0].name}') -o jsonpath='{.data.token}' | base64 --decode)
```
### Login Openshift with token
```
oc login --token $TOKEN https://api.mycluster.mydomain.com:6443 --insecure-skip-tls-verify
```
