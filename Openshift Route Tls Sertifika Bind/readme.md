## Openshift Route Tls Sertifika Bind

##### **First Way**
```bash
oc create route edge login-route --service=login --cert=/root/cert/MyCert.local.crt --key=/root/cert/MyCert.local.rsa --ca-cert=/root/allinone.crt --hostname=MywebsiteURL.local
```
Note: allinone.crt -> combination of root and ca certificates

##### **Second Way**
  

**Route types**


- **No TLS (port 80)** : Non-encrypted HTTP traffic

- **Edge (port 443)** : Encrypted HTTPS traffic between the client and the router proxy . The pod exposes non-encrypted HTTP endpoint.


- **Re-Encrypt (port 443)** : Encrypted traffic is terminated by the router proxy just like for edge routes, but the pod also exposes an HTTPS endpoint. So there is an another TLS connection between the proxy and the pod.  

- **Passthrough (port 443)** : The router is not involved in TLS offloading. The traffic is encrypted end-to-end between the client and the pod. This type can be used for non-HTTP TLS endpoints as well.

![image](https://user-images.githubusercontent.com/3519706/80637941-69886600-8a68-11ea-8915-65292b5bb501.png)

![image](https://user-images.githubusercontent.com/3519706/80638060-8de44280-8a68-11ea-898a-c43f0afb2c4c.png)

![image](https://user-images.githubusercontent.com/3519706/80638097-9b99c800-8a68-11ea-9c02-2782876a4ece.png)

![image](https://user-images.githubusercontent.com/3519706/80638351-f92e1480-8a68-11ea-95f7-bad6c81b9ae9.png)

![image](https://user-images.githubusercontent.com/3519706/80638570-5033e980-8a69-11ea-862a-67aba014007d.png)

![image](https://user-images.githubusercontent.com/3519706/80638691-84a7a580-8a69-11ea-9769-260e9e7b0829.png)

![image](https://user-images.githubusercontent.com/3519706/80638797-b02a9000-8a69-11ea-9609-36187c1fe519.png)