### Openshift Ingress Certificate

- You must have a wildcard certificate for the fully qualified .apps subdomain and its corresponding private key. Each should be in a separate PEM format file.

- The private key must be unencrypted. If your key is encrypted, decrypt it before importing it into OpenShift Container Platform.

- The certificate must include the subjectAltName extension showing *.apps.<clustername>.<domain>.

- The certificate file can contain one or more certificates in a chain. The wildcard certificate must be the first certificate in the file. It can then be followed with any intermediate certificates, and the file should end with the root CA certificate.

- Copy the root CA certificate into an additional PEM format file.

**Check the existence of the custom-ca configmap**
```sh
oc get configmap -n openshift-config --field-selector=metadata.name="custom-ca" -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the custom-ca configmap if it was not found**
```sh
oc create configmap "custom-ca" --from-file=ca-bundle.crt="certs/api-ca.pem" -n openshift-config
```
**Check the current configured Trusted CA**
```sh
oc get proxy/cluster -o=jsonpath="{.spec.trustedCA.name}"
```
**Update the cluster Trusted CA if not configured**
```sh
oc patch proxy/cluster --type=merge -p '{"spec":{"trustedCA":{"name":"custom-ca"}}}'
```
**Check the newly configured trusted CA**
```sh
oc get proxy/cluster -o=jsonpath="{.spec.trustedCA.name}"
```
**Check the existence of the cluster Ingress secret**
```sh
oc get secrets -n openshift-ingress --field-selector=metadata.name="myclusterName-ingress-tls" -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the cluster Ingress secret if it was not found**
```sh
oc create secret tls "myclusterName-ingress-tls" --cert="cert/ingress-cert.pem" --key="cert/ingress-key.pem" -n openshift-ingress
```
**Check the current configured Ingress-operator certificate secret**
```sh
oc get ingresscontroller.operator default -n openshift-ingress-operator -o=jsonpath="{.spec.defaultCertificate.name}"
```
**Update the cluster Ingress-operator Certificate if not configured**
```sh
oc patch ingresscontroller.operator default --type=merge -p '{"spec":{"defaultCertificate":{"name":"myclusterName-ingress-tls"}}}' -n openshift-ingress-operator
```
**Check the newly configured Ingress-operator certificate secret**
```sh
oc get ingresscontroller.operator default -n openshift-ingress-operator -o=jsonpath="{.spec.defaultCertificate.name}"
```
 **reference:**

[+] https://docs.openshift.com/container-platform/4.7/security/certificates/replacing-default-ingress-certificate.html
