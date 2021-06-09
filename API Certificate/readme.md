
### Openshift API Certificate

- You must have a certificate for the FQDN and its corresponding private key. Each should be in a separate PEM format file.

- The private key must be unencrypted. If your key is encrypted, decrypt it before importing it into OpenShift Container Platform

- The certificate must include the subjectAltName extension showing the FQDN.

  
**Check the existence of the cluster API secret**
```sh
oc get secrets -n openshift-config --field-selector=metadata.name="mycluster-apiserver-tls" -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the cluster API secret if it was not found**
```sh
oc create secret tls "mycluster-apiserver-tls" --cert="certs/api-cert.pem" --key="certs/api-key.pem" -n openshift-config
```
**Collect the cluster API hostname**
```sh
api_hostname=$(oc whoami --show-server | sed -r "s|.*//(.*):.*|\1|")
```
**Check the current configured API-server certificate secret**
```sh
oc explain apiservers.spec.servingCerts.namedCertificates
```
```sh
oc get apiservers.config.openshift.io cluster -o=jsonpath="{.spec.servingCerts.namedCertificates[*].servingCertificate.name}"
```
**Update the cluster API-server Certificate if not configured**
```sh
oc patch apiservers.config.openshift.io cluster --type=merge -p '{"spec":{"servingCerts":{"namedCertificates":[{"names":["$api_hostname"], "servingCertificate":{"name":"mycluster-apiserver-tls"}}]}}}'
```
**watch the readiness of the API-server, if API-server Certificate was configured**
```sh
url https://$api_hostname:6443/healthz --cacert certs/api-ca.pem
```
**Update the client kubeconfig with the CA, if API-server Certificate was configured**
```sh
oc config set-cluster api-mycluster-mydomain-com-tr:6443 --certificate-authority=certs/api-ca.pem
```
**Install CA on the local system, if API-server Certificate was configured**
```sh
cp certs/api-ca.pem /etc/pki/ca-trust/source/anchors; update-ca-trust
```
**watch the readiness of the kube-apiserver operator, if API-server Certificate was configured**
```sh
oc get co kube-apiserver -o jsonpath={.status.conditions[1].status}
```
**Collect the default project name as a test, if API-server Certificate was configured**
```sh
oc get project default -o=jsonpath="{.metadata.name}"
```
**reference:**

[+] https://docs.openshift.com/container-platform/4.7/security/certificates/api-server.html
