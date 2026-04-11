# LDAP Authentication Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- LDAP server + bind credentials
- LDAP CA cert (for LDAPS)

## Step 1: Bind Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ldap-secret
  namespace: openshift-config
type: Opaque
stringData:
  bindPassword: "your-ldap-bind-password"
```
```bash
oc apply -f ldap-secret.yaml
```

## Step 2: CA ConfigMap (LDAPS)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ldap-ca-cert
  namespace: openshift-config
data:
  ca.crt: |
    -----BEGIN CERTIFICATE-----
    <YOUR_LDAP_CA_CERTIFICATE>
    -----END CERTIFICATE-----
```
```bash
oc apply -f ldap-ca.yaml
```

## Step 3: OAuth Configuration
```yaml
apiVersion: config.openshift.io/v1
kind: OAuth
metadata:
  name: cluster
spec:
  identityProviders:
  - name: ldap
    type: LDAP
    mappingMethod: claim
    ldap:
      attributes:
        id: [ dn ]
        email: [ mail ]
        name: [ cn ]
        preferredUsername: [ sAMAccountName ]
      bindDN: "CN=svc_ldap_bind,OU=Service Accounts,DC=example,DC=com"
      bindPassword:
        name: ldap-secret
      ca:
        name: ldap-ca-cert
      insecure: false
      url: "ldaps://ldap.example.com:636/DC=example,DC=com?sAMAccountName?sub?(objectClass=person)"
```
```bash
oc apply -f oauth-ldap.yaml
```

## Step 4: Wait for Pods
```bash
oc get pods -n openshift-authentication -w
oc get co authentication
```

## Step 5: Test Login
```bash
oc login -u <ldap-username> https://api.cluster.example.com:6443
oc whoami
```

## Verification
```bash
oc get oauth cluster -o yaml
oc get pods -n openshift-authentication
oc get co authentication
oc get identity
oc get users
```

## Troubleshooting
```bash
oc logs -n openshift-authentication <oauth-pod>
oc logs -n openshift-authentication-operator <operator-pod>
oc run ldap-test --image=registry.access.redhat.com/ubi8/ubi --rm -it -- bash -c "curl -v ldaps://ldap.example.com:636"
oc run ldap-test --image=registry.access.redhat.com/ubi8/ubi --rm -it -- bash -c "ldapsearch -H ldaps://ldap.example.com:636 -D 'CN=svc_ldap_bind,OU=Service Accounts,DC=example,DC=com' -w 'password' -b 'DC=example,DC=com' '(sAMAccountName=testuser)'"
oc get secret ldap-secret -n openshift-config -o yaml
```

## LDAP URL Formats
```
ldaps://server:port/baseDN?attribute?scope?filter
ldaps://ad.example.com:636/DC=example,DC=com?sAMAccountName?sub?(objectClass=person)
ldaps://openldap.example.com:636/ou=people,dc=example,dc=com?uid?sub?(objectClass=inetOrgPerson)
```

## Disable LDAP
```bash
oc patch oauth cluster --type=json -p='[{"op":"remove","path":"/spec/identityProviders/0"}]'
# Or reset
oc apply -f - <<'EOF'
apiVersion: config.openshift.io/v1
kind: OAuth
metadata:
  name: cluster
spec:
  identityProviders: []
EOF
```

## Notes
- Pods restart on config change
- Users created on first login
- Group sync separate procedure
- Use LDAPS for security
