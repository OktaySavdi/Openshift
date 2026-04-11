# LDAP Group Sync Configuration - OpenShift CLI Procedure

## Prerequisites
- LDAP authentication configured
- Cluster-admin access
- Bind credentials with group read

## Step 1: Sync ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ldap-sync-config
  namespace: openshift-config
data:
  ldap-sync.yaml: |
    kind: LDAPSyncConfig
    apiVersion: v1
    url: ldaps://ldap.example.com:636
    bindDN: "CN=svc_ldap_bind,OU=Service Accounts,DC=example,DC=com"
    bindPassword:
      file: "/etc/secrets/bindPassword"
    insecure: false
    ca: /etc/ldap-ca/ca.crt
    rfc2307:
      groupsQuery:
        baseDN: "OU=Groups,DC=example,DC=com"
        scope: sub
        derefAliases: never
        filter: (objectClass=group)
      groupUIDAttribute: dn
      groupNameAttributes: [ cn ]
      groupMembershipAttributes: [ member ]
      usersQuery:
        baseDN: "DC=example,DC=com"
        scope: sub
        derefAliases: never
      userUIDAttribute: dn
      userNameAttributes: [ sAMAccountName ]
```

## Step 2: Group Whitelist (Optional)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ldap-group-whitelist
  namespace: openshift-config
data:
  whitelist.txt: |
    CN=OpenShift-Admins,OU=Groups,DC=example,DC=com
    CN=OpenShift-Developers,OU=Groups,DC=example,DC=com
    CN=OpenShift-Viewers,OU=Groups,DC=example,DC=com
```

## Step 3: Service Account
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: ldap-group-syncer
  namespace: openshift-config
```

## Step 4: ClusterRole
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: ldap-group-syncer
rules:
- apiGroups: ["", "user.openshift.io"]
  resources: ["groups"]
  verbs: ["get","list","create","update"]
```

## Step 5: ClusterRoleBinding
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ldap-group-syncer
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: ldap-group-syncer
subjects:
- kind: ServiceAccount
  name: ldap-group-syncer
  namespace: openshift-config
```

## Step 6: CronJob
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: ldap-group-sync
  namespace: openshift-config
spec:
  schedule: "0 */6 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: ldap-group-syncer
          restartPolicy: OnFailure
          containers:
          - name: ldap-sync
            image: registry.redhat.io/openshift4/ose-cli:latest
            command:
            - /bin/bash
            - -c
            - |
              oc adm groups sync --sync-config=/etc/config/ldap-sync.yaml --whitelist=/etc/whitelist/whitelist.txt --confirm
            volumeMounts:
            - name: ldap-sync-config
              mountPath: /etc/config
            - name: ldap-bind-password
              mountPath: /etc/secrets
            - name: ldap-ca
              mountPath: /etc/ldap-ca
            - name: whitelist
              mountPath: /etc/whitelist
          volumes:
          - name: ldap-sync-config
            configMap:
              name: ldap-sync-config
          - name: ldap-bind-password
            secret:
              secretName: ldap-secret
          - name: ldap-ca
            configMap:
              name: ldap-ca-cert
          - name: whitelist
            configMap:
              name: ldap-group-whitelist
```

## Manual Sync
```bash
oc adm groups sync --sync-config=ldap-sync.yaml
oc adm groups sync --sync-config=ldap-sync.yaml --whitelist=whitelist.txt
oc adm groups sync --sync-config=ldap-sync.yaml --whitelist=whitelist.txt --confirm
oc adm groups prune --sync-config=ldap-sync.yaml --whitelist=whitelist.txt --confirm
```

## Verification
```bash
oc get cronjob -n openshift-config
oc get jobs -n openshift-config
oc logs -n openshift-config job/ldap-group-sync-<job-id>
oc get groups
oc get group <group-name> -o yaml
```

## Troubleshooting
```bash
oc create job --from=cronjob/ldap-group-sync manual-sync -n openshift-config
oc logs -n openshift-config -l job-name=manual-sync
oc auth can-i create groups --as=system:serviceaccount:openshift-config:ldap-group-syncer
```

## Schedule Examples
- Every 12h: `0 */12 * * *`
- Daily midnight: `0 0 * * *`
- Weekly Sun 2AM: `0 2 * * 0`

## Delete Sync
```bash
oc delete cronjob ldap-group-sync -n openshift-config
oc delete clusterrolebinding ldap-group-syncer
oc delete clusterrole ldap-group-syncer
```

## Notes
- Groups auto-created & updated
- Whitelist controls scope
- Prune removes stale groups
