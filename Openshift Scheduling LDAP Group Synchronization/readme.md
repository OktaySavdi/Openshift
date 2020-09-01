## Scheduling LDAP Group Synchronization

The following steps detail how to create a CronJob to perform a periodic LDAP group synchronization:

• Store the LDAP bind password in an OpenShift Secret so the CronJob can access the password in a secure way.

• Store the LDAPSyncConfig and the IdM certificate in a ConfigMap so the CronJob can use them.

Create a new LDAPSyncConfig:
```yaml
kind: LDAPSyncConfig
apiVersion: v1
url: ldaps://idm.ocp4.example.com
bindDN: uid=admin,cn=users,cn=accounts,dc=example,dc=com
bindPassword:
  file: /etc/secrets/bindPassword
insecure: false
ca: /etc/config/ca.crt
rfc2307:
    groupsQuery:
        baseDN: "cn=groups,cn=accounts,dc=example,dc=com"
        scope: sub
        derefAliases: never
        pageSize: 0
        filter: (objectClass=ipausergroup)
    groupUIDAttribute: dn
    groupNameAttributes: [ cn ]
    groupMembershipAttributes: [ member ]
    usersQuery:
        baseDN: "cn=users,cn=accounts,dc=example,dc=com"
        scope: sub
        derefAliases: never
        pageSize: 0
    userUIDAttribute: dn
    userNameAttributes: [ uid ]
```
Create a Secret:
```ruby
oc create secret generic ldap-secret --from-literal bindPassword=1234
```
Create a ConfigMap:
```ruby
oc create configmap ldap-config --from-file ldap-group-sync.yaml=tmp/ldap-sync-config-cronjob.yml,ca.crt=tmp/ca.crt
```
Create a CronJob:
```yaml
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: group-sync
spec:
  schedule: "*/1 * * * *"
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: Never
          containers:
          - name: group-sync
            image: quay.io/openshift/origin-cli
            command:
            - /bin/sh
            - -c
            - oc adm groups sync --sync-config /etc/config/ldap-group-sync.yaml --confirm
            volumeMounts:
            - mountPath: "/etc/config"
              name: "ldap-sync-volume"
            - mountPath: "/etc/secrets"
              name: "ldap-bind-password"
          volumes:
            - name: "ldap-sync-volume"
              configMap:
                name: ldap-config
            - name: "ldap-bind-password"
              secret:
                secretName: ldap-secret
          serviceAccountName: ldap-group-syncer-sa
          serviceAccount: ldap-group-syncer-sa
```
Schedule the job to run every minute to have shorter test cycles.
Provide an OCI image to run the **CronJob**. This image must contain the oc command.
The command to execute in the **CronJob** is the sync command with **--confirm**.
Provide the Secret and **ConfigMap** to the **CronJob**.
Provide a **ServiceAccount**, which can execute **get, list, create, update** verbs on the **groups** resource.

**Verify LDAP Group Synchronization**

Use the watch command to inspect the CronJob execution:
```ruby
watch oc get cronjobs,jobs,pods
```
