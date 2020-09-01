cert

## Integrating OpenShift with an Enterprise Certificate Authority


You should be able to:

• Inspect a new certificate signed by the classroom certificate authority.

• Configure the ingress controller to use the new certificate.

• Configure the master API to use the new certificate.

• Validate the new certificate by accessing the web console and by running the oc login command.

Run the **openssl** command to view details about the certificate. The certificate is a wildcard for any URL ending in **.apps.ocp4.example.com** It also covers the master API URL **api.ocp4.example.com**.
```ruby
openssl x509 -in  wildcard-api.pem --noout -subject -issuer -ext 'subjectAltName'
```
Create an additional certificate combining the new certificate and the classroom CA certificate. Add comments to make it easier to identify the certificates.
```ruby
cat wildcard-api.pem /etc/pki/tls/certs/ca.pem > combined-cert.pem
```
Create a new TLS secret in the **openshift-config** namespace using **combinedcert.pem** and **wildcard-api-key.pem**.
```ruby
oc create secret tls custom-tls --cert combined-cert.pem --key wildcard-api-key.pem -n openshift-config
```
Modify the **apiserver-cluster.yaml** file.
```ruby
vi apiserver-cluster.yaml
```
Update the file to match the bold lines below. Specify the URL of the master API and the name of the TLS secret that you just created.
```yaml
apiVersion: config.openshift.io/v1
kind: APIServer
metadata:
  name: cluster
spec:
  servingCerts:
    namedCertificates:
    - names:
    - api.ocp4.example.com # set adress
    servingCertificate:
      name: custom-tls
```
Apply the changes using the oc apply command.
```ruby
oc apply -f apiserver-cluster.yaml
```
If the change is successful, then the **kube-apiserver** pods in the **openshift-kubeapiserver** namespace redeploy. It can take five minutes or more before the pods are ready. Wait until the **PROGRESSING** column for the **kube-apiserver** cluster operator has a value of **True**. Press **Ctrl+C** to exit the **watch** command.
```ruby
watch oc get co/kube-apiserver
```
![image](https://user-images.githubusercontent.com/3519706/91289612-3e561f00-e79b-11ea-80f3-6fe773a69109.png)


![image](https://user-images.githubusercontent.com/3519706/91288567-ce936480-e799-11ea-93d7-20eef213da3d.png)

Create a new configuration map in the **openshift-config** namespace using the combined certificate. Ensure that the configuration map uses a data key of **cabundle.crt**
```ruby
oc create configmap combined-certs --from-file ca-bundle.crt=combined-cert.pem -n openshift-config
```
Modify the cluster proxy so that it adds the new configuration map to the trusted certificate bundle.
```ruby
vi proxy-cluster.yaml
```
Update the file to match the bold line below. Specify the name of the configuration map that you just created.
```yaml
apiVersion: config.openshift.io/v1
kind: Proxy
metadata:
  name: cluster
spec:
  trustedCA:
    name: combined-certs
```
Apply the changes using the oc apply command.
```ruby
oc apply -f proxy-cluster.yaml
```
Create a TLS secret named **custom-tls-bundle** in the **openshift-ingress**
namespace. Use custom-combined-certs as the certificate and **wildcard-apikey.pem** as the key.
```ruby
oc create secret tls custom-tls-bundle --cert combined-cert.pem --key wildcard-api-key.pem -n openshift-ingress
```
Modify the ingress controller operator to use the **custom-tls-bundle** secret. A successful change redeploys the router pods in the **openshift-ingress** namespace.
```ruby
vi ingresscontrollers.yaml
```
Update the file to match the bold line below. Specify the name of the TLS secret that you just created.
```yaml
apiVersion: operator.openshift.io/v1
kind: IngressController
metadata:
  finalizers:
  - ingresscontroller.operator.openshift.io/finalizer-ingresscontroller
  name: default
  namespace: openshift-ingress-operator
spec:
  defaultCertificate:
    name: custom-tls-bundle
```
Apply the changes using the oc apply command.
```ruby
oc apply -f ingresscontrollers.yaml
```
Wait until the new **router-default** pods in the **openshift-ingress** namespace finish redeploying and the previous router pods disappear. Press **Ctrl+C** to exit the watch command.
```ruby
watch oc get pods -n openshift-ingress
```
Identify the OpenShift web console URL.
```ruby
oc whoami --show-console
```
Previously in this exercise, the **kube-apiserver** pods in the **openshift-kubeapiserver** namespace were redeployed. Confirm that the redeployment finished, and then test the certificate used by the OpenShift master API.

Wait until the **PROGRESSING** column for the **kube-apiserver** cluster operator has a value of **False**. Press **Ctrl+C** to exit the **watch** command.
```ruby
watch oc get co/kube-apiserver
```
![image](https://user-images.githubusercontent.com/3519706/91289565-30080300-e79b-11ea-8328-c83c8278ce34.png)

Log out of OpenShift.
```ruby
oc logout
```
Remove the existing .kube/ directory.
```ruby
rm -rf ~/.kube/
```
oc login -u admin -p **** https://api.ocp4.example.com:6443
Login successful.

![image](https://user-images.githubusercontent.com/3519706/91289905-9e4cc580-e79b-11ea-9176-8e599a024439.png)
