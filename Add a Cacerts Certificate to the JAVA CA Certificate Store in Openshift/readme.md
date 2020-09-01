## Add a Cacerts Certificate to the JAVA CA Certificate Store in Openshift

**cacert file is identified as secret**
```ruby
oc create secret generic cacerts --from-file=/root/cacerts
```
**Control is made on the Console that a secret has been created**

![image](https://user-images.githubusercontent.com/3519706/89736510-95a58100-da72-11ea-8c72-4cdace1918c3.png)

**Mounted inside the secret deployment**
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
or
```ruby
oc set volume dc/<DC-NAME> -t configmap --name cacerts --add --read-only=true --mount-path /externalfilesca --configmap-name cacerts
```

![image](https://user-images.githubusercontent.com/3519706/89736097-b7e9cf80-da6f-11ea-91ee-949572ec39e3.png)

Env information is defined for cacerts
```
NAME : JAVA_OPTIONS
VALUE : -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStore=/externalfilesca/cacerts
```
```ruby
oc set env dc/<DC-NAME> JAVA_OPTIONS="-Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStore=/externalfilesca/cacerts"
```
![image](https://user-images.githubusercontent.com/3519706/89736338-59bdec00-da71-11ea-92ce-ca8b8bb049c5.png)

Pods > **Select Pod** > Terminals

keytool -list -keystore /externalfilesca/cacerts -storepass changeit

![image](https://user-images.githubusercontent.com/3519706/89736406-dc46ab80-da71-11ea-8c48-1d00a6745247.png)
