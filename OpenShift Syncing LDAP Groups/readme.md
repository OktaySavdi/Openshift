## OpenShift Syncing LDAP Groups

#### Create yaml sync_ldap.yaml file
```
vi sync_ldap.yaml
```
```yaml
kind: LDAPSyncConfig 
apiVersion: v1 
url: ldaps://MyLdapServer:636 
bindDN: "CN=MyUser,OU=MyOU,DC=MyDomain,DC=local" 
bindPassword: MyPassword 
ca: /root/cer/root.crt 
groupUIDNameMapping: 
  "CN=MyGroupName,OU=MyOU,DC=MyDomain,DC=local": MyTeam  
insecure: false 
rfc2307: 
    groupsQuery: 
        baseDN: "CN=MyGroupName,OU=MyOU,DC=MyDomain,DC=local"  # the address of the group to be added is given 
        scope: sub 
        derefAliases: never 
        filter: (objectclass=*) 
        pageSize: 0 
    groupUIDAttribute: dn 
    groupNameAttributes: [ cn ] 
    groupMembershipAttributes: [ member ] 
    usersQuery: 
        baseDN: "OU=MyUserOU,DC=MyDomain,DC=local" #the address of users connected to the group is given 
        scope: sub 
        derefAliases: never 
        pageSize: 0 
    userUIDAttribute: dn 
    userNameAttributes: [ sAMAccountName ] 
    tolerateMemberNotFoundErrors: false 
    tolerateMemberOutOfScopeErrors: false
```

**# Add LDAP group on openshift**
```
oc adm groups sync --sync-config=sync_ldap.yaml --confirm --loglevel=5
```
**# Remove LDAP group on openshift**
```
oc adm groups prune --sync-config=sync_ldap.yaml --confirm
```
**# List openshift groups**
```
oc get groups
```
**# Set permission group for project**
```
oc policy add-role-to-group -n <Project Name> view ArakatmanYonetimi
```
**# Check policybindings on project**
```
oc describe -n <Project Name> policybindings
```