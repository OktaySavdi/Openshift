## Troubleshooting Certificates in OpenShift

If the API server certificate expires, you can still log in to the web console or use the CLI, however,
the following message will appear:
```bash
The server is using an invalid certificate: x509: certificate has expired or is
not yet valid
```
If you add a certificate for the API server (when it previously used the default ingress controller
certificate), or you remove a specific API server certificate, then the kube-apiserver pods in
the **openshift-kube-apiserver** namespace redeploy.

**## Renewing the API Server Certificate**

To renew the API server certificate, you must identify the name of the secret containing the
certificate used by the API server.
```yaml
[user@demo ~]$ oc get apiserver/cluster -o yaml
...output omitted...
spec:
  servingCerts:
    namedCertificates:
    - names:
      - <API-URL>
      servingCertificate:
        name: <SECRET-NAME>
```
1- The API server URL.

2- The name of the secret that contains the certificate.

Extract the secret, and then use the openssl command to inspect the certificate.
```shell
[user@demo ~]$ oc extract secret/<SECRET-NAME> -n openshift-config --confirm
tls.crt
tls.key
[user@demo ~]$ openssl x509 -in tls.crt -noout -dates
notBefore=Apr 15 21:12:55 2015 GMT
notAfter=Apr 15 21:13:55 2020 GMT
```
If the certificate has expired or will expire soon, then follow your company procedures to request
a new certificate. As with creating the initial certificate for the API server, the certificate signing
request for the certificate renewal must contain the **subjectAltName** extension for the URL
used to access the API server, such as **DNS:api.ocp.example.com.**

After obtaining a new certificate from your organization administrator, you can renew the
certificate in place by running the following command.
```shell
[user@demo ~]$ oc set data secret <SECRET-NAME> \
> --from-file tls.crt=<PATH-TO-NEW-CERTIFICATE> \
> --from-file tls.key=<PATH-TO-KEY> \
> -n openshift-config
```
**## Renewing the Ingress Controller Certificate**

The OpenShift ingress controller manages routes for internal services, such as OAuth, the web
console, and Prometheus. The ingress controller might use the cluster proxy, which also relies on
certificates.

To renew the ingress controller certificate, you must identify the name of the secret containing the
certificate used by the ingress controller.
```shell
[user@demo ~]$ oc get ingresscontroller/default -n openshift-ingress-operator \
> -o jsonpath='{.spec.defaultCertificate.name}{"\n"}'
<SECRET-NAME>
```
Extract the secret, and then use the openssl command to inspect the certificate.
```shell
[user@demo ~]$ oc extract secret/<SECRET-NAME> -n openshift-ingress --confirm
tls.crt
tls.key
[user@demo ~]$ openssl x509 -in tls.crt -noout -dates
notBefore=May 15 11:12:23 2018 GMT
notAfter=May 15 11:13:23 2020 GMT
```
If the certificate has expired or will expire soon, follow your company procedures to request a new
certificate. As with creating the initial wildcard certificate, the certificate signing request for the
certificate renewal must contain the **subjectAltName** extension of ***.apps.OPENSHIFTDOMAIN**,
such as ***.apps.ocp.example.com**. After obtaining a new certificate, the secret can
be updated in place using the **oc set data secret** command.
```shell
[user@demo ~]$ oc set data secret <SECRET-NAME> \
> --from-file tls.crt=<PATH-TO-NEW-CERTIFICATE> \
> --from-file tls.key=<PATH-TO-KEY> \
> -n openshift-config
```
Updating the certificate instructs the router pods to redeploy in the **openshift-ingress**
namespace.

After the **router-default** pods finish redeploying, you can use the following **curl** command to
confirm the renewal of the certificate. Although this example checks the certificate for the OAuth
URL, the same certificate is used for the web console, Prometheus, Grafana, and more.
```shell
[user@demo ~]$ curl -k -v \
> https://oauth-openshift.apps.ocp.example.com 2>&1 | grep -w date
* start date: Jul 21 18:15:22 2020 GMT
* expire date: Jul 21 18:15:22 2022 GMT
```
If the cluster proxy uses the same certificate, then the configuration map identified for the cluster
proxy must be updated as well. Identify the name of the configuration map used by the cluster
proxy.
```shell
[user@demo ~]$ oc get proxy/cluster -o jsonpath='{.spec.trustedCA.name}{"\n"}'
```
The configuration map exists in the **openshift-config** namespace.

The configuration map can be updated in place using the **oc set data configmap** command.
```shell
[user@demo ~]$ oc set data configmap <CONFIGMAP-NAME> \
> --from-file ca-bundle.crt=<PATH-TO-NEW-CERTIFICATE> -n openshift-config
```
**## Troubleshooting Certificates Renewal**

Should the certificate not update in the cluster, check the following:

• Use the openssl command to ensure that the new certificate is valid.

• Verify that the notBefore date is in the past and the notAfter date is in the future.
```shell
[user@demo ~]$ openssl x509 -in <PATH-TO-CERTIFICATE> -noout -dates
notBefore=Jul 21 19:07:50 2020 GMT
notAfter=Jul 19 19:07:50 2025 GMT
```
After updating a certificate, you can compare the certificate serial number of the secret with the
certificate file to make sure that they match. A mismatch indicates that the secret did not update
properly.
```shell
[user@demo ~]$ oc get secret <SECRET-NAME> -n openshift-config \
> -o jsonpath='{.data.*}' | base64 -di | openssl x509 -noout -serial
serial=7730293A5E0590039EC8B94B85954C4DFC8CEB60
[user@demo ~]$ openssl x509 -in <PATH-TO-CERTIFICATE> -noout -serial
serial=7730293A5E0590039EC8B94B85954C4DFC8CEB60
```
**## Troubleshooting API Server Certificates**

The **kube-apiserver** pods do not redeploy for an in place certificate update. Run the **oc get
events** command in the **openshift-kube-apiserver** namespace to verify that the **kubeapiserver**
pods use the updated certificate.
```shell
[user@demo ~]$ oc get events --sort-by='.lastTimestamp' \
> -n openshift-kube-apiserver
...output omitted...
30s ... CertificateUpdated ... pod/kube-apiserver-master01 ... Wrote updated
secret: openshift-kube-apiserver/user-serving-cert-000
30s ... CertificateUpdated ... pod/kube-apiserver-master02 ... Wrote updated
secret: openshift-kube-apiserver/user-serving-cert-000
30s ... CertificateUpdated ... pod/kube-apiserver-master03 ... Wrote updated
secret: openshift-kube-apiserver/user-serving-cert-000
```
**## Troubleshooting Ingress Controller Certificates**

For the ingress controller, ensure that new **router-default** pods in the **openshift-ingress**
namespace finished redeploying. Expect that each router pod has a status of **Running** and that
each pod displays **1/1** in the **READY** column. Also expect that router pods associated with a
previous replica set are not displayed.
```shell
[user@demo ~]$ oc get pods -n openshift-ingress
NAME READY STATUS RESTARTS AGE
router-default-55df6c587-96dhv 1/1 Running 0 9m57s
router-default-55df6c587-bf4qp 1/1 Running 0 9m24s
```
