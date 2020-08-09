## Add a Cacerts Certificate to the JAVA CA Certificate Store in Openshift

**cacert file is identified as secret**
```ruby
oc create secret generic cacert --from-file=/root/cacert
```
Control is made on the Console that a secret has been created
![image](https://user-images.githubusercontent.com/3519706/89735550-009f8980-da6c-11ea-9359-d1c8a3c36a6e.png)

Mounted inside the secret deployment
```yaml
spec:
  containers:
    - name: busybox
      image: busybox
      volumeMounts:
        - name: java-certs
          mountPath: /externalfilesca
  volumes:
    - name: java-certs
      secret:
        secretName: cacerts
```

![image](https://user-images.githubusercontent.com/3519706/89736097-b7e9cf80-da6f-11ea-91ee-949572ec39e3.png)

Env information is defined for cacerts
```
NAME : JAVA_OPTIONS
VALUE : -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStore=/externalfilesca/cacerts
```
```ruby
oc set env deployment/python JAVA_OPTIONS="-Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStore=/externalfilesca/cacerts"
```
![image](https://user-images.githubusercontent.com/3519706/89736338-59bdec00-da71-11ea-92ce-ca8b8bb049c5.png)

Pods > **Select Pod** > Terminals

keytool -list -keystore /externalfilesca/cacerts -storepass changeit

![image](https://user-images.githubusercontent.com/3519706/89736406-dc46ab80-da71-11ea-8c48-1d00a6745247.png)
