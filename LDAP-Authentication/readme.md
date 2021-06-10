## Openshift LDAP Authentication

LDAP uses bind operations to authenticate applications, and you can integrate your OpenShift Container Platform cluster to use LDAPv3 authentication. Configuring LDAP authentication allows users to log in to OpenShift Container Platform with their LDAP credentials.

During authentication, the LDAP directory is searched for an entry that matches the provided user name. If a single unique match is found, a simple bind is attempted using the distinguished name (DN) of the entry plus the provided password.

Define an OpenShift Container Platform Secret that contains the bindPassword.
```bash
oc create secret generic ldap-secret --from-literal=bindPassword=<secret> -n openshift-config  
```
Define an OpenShift Container Platform ConfigMap containing the certificate authority by using the following command. The certificate authority must be stored in the ca.crt key of the ConfigMap.
```
oc create configmap ca-config-map --from-file=ca.crt=/path/to/ca.crt -n openshift-config
``` 
### Get-ADUser with distinguishedname
``` 
dsquery user -name <username>

or

(Get-ADUser <username>).DistinguishedName
``` 
The following yaml file is applied.
```
vi ldap.yaml
```
```yaml
#ldap.yaml 
apiVersion: config.openshift.io/v1 
kind: OAuth 
metadata: 
  name: cluster 
spec: 
  identityProviders: 
  - name: ldap 
    mappingMethod: claim 
    type: LDAP 
    ldap: 
      attributes: 
        id: 
        - sAMAccountName 
        email: 
        - mail 
        name: 
        - cn 
        preferredUsername: 
        - sAMAccountName 
      bindDN: "CN=MyUser,OU=MyOu,DC=MyDomain,DC=local" 
      bindPassword: 
        name: ldap-secret 
      ca: 
        name: ca-config-map 
      insecure: false 
      url: "ldaps://ldaps.MyDomain.local:636/CN=MyUser,OU=MyOU,DC=MyDomain,DC=local?sAMAccountName"
```

apply yaml file
```
oc apply -f ldap.yaml
```

check Project pods
```
oc project openshift-authentication
```
oc logs <pods>
```
I0410 13:27:30.444596       1 secure_serving.go:64] Forcing use of http/1.1 only
I0410 13:27:30.444713       1 secure_serving.go:123] Serving securely on [::]:6443
```
