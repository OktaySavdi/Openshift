### CronJob LDAP Sync with CronJobs
```yaml
kind: ServiceAccount
apiVersion: v1
metadata:
  name: ldap-group-syncer
  namespace: openshift-authentication
  labels:
    app: cronjob-ldap-group-sync
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: ldap-group-syncer
  labels:
    app: cronjob-ldap-group-sync
rules:
  - apiGroups:
      - ''
      - user.openshift.io
    resources:
      - groups
    verbs:
      - get
      - list
      - create
      - update
---
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: ldap-group-syncer
  labels:
    app: cronjob-ldap-group-sync
subjects:
  - kind: ServiceAccount
    name: ldap-group-syncer
    namespace: openshift-authentication
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: ldap-group-syncer
---
apiVersion: v1
data:
  bindPassword: < MY_LDAP_USER_PASSWORD - base64>
  ca.crt: SUZJQ0SDKFHDFPRUGFFJVNCVERGHREGHFKDJVNHFGOREGKJVBKDVHBEIBVEKFURS
kind: Secret
metadata:
  name: ldap-bind-cert
  namespace: openshift-authentication
---
kind: ConfigMap
apiVersion: v1
metadata:
  name: ldap-group-syncer
  namespace: openshift-authentication
  labels:
    app: cronjob-ldap-group-sync
data:
  ldap-group-sync.yaml: |
    kind: LDAPSyncConfig
    apiVersion: v1
    url: ldap://ad.example.com:389
    bindDN: CN=user-ocp,OU=Users,DC=ad,DC=example,DC=com
    bindPassword:
      file: /etc/secrets/bindPassword
    insecure: false
    ca: /etc/secrets/ca.crt
    rfc2307:
        groupsQuery:
          baseDN: "DC=ad,DC=example,DC=com"
          scope: sub
          derefAliases: never
          filter: (objectclass=group)
        groupUIDAttribute: dn
        groupNameAttributes: [ cn ]
        groupMembershipAttributes: [ member ]
        usersQuery:
            baseDN: "DC=ad,DC=example,DC=com"
            scope: sub
            derefAliases: never
            pageSize: 0
        userUIDAttribute: dn
        userNameAttributes: [ sAMAccountName ]
        tolerateMemberNotFoundErrors: true
        tolerateMemberOutOfScopeErrors: true
---
kind: ConfigMap
apiVersion: v1
metadata:
  name: ldap-group-syncer-whitelist
  namespace: openshift-authentication
  labels:
    app: cronjob-ldap-group-sync
data:
  whitelist.txt: |
    CN=Cluster-Admins,CN=users,DC=ad,DC=example,DC=com
    CN=Developers,CN=users,DC=ad,DC=example,DC=com
---
kind: CronJob
apiVersion: batch/v1beta1
metadata:
  name: ldap-group-syncer
  namespace: openshift-authentication
  labels:
    app: cronjob-ldap-group-sync
spec:
  schedule: "0 0 * * *"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 5
  failedJobsHistoryLimit: 5
  jobTemplate:
    metadata:
      labels:
        app: cronjob-ldap-group-sync
    spec:
      backoffLimit: 0
      template:
        metadata:
          labels:
            app: cronjob-ldap-group-sync
        spec:
          containers:
            - name: ldap-group-sync
              image: "registry.redhat.io/openshift4/ose-cli:v4.7"
              command:
                - "/bin/bash"
                - "-c"
                - oc adm groups sync --whitelist=/etc/whitelist/whitelist.txt --sync-config=/etc/config/ldap-group-sync.yaml --confirm
              volumeMounts:
                - mountPath: "/etc/config"
                  name: "ldap-sync-volume"
                - mountPath: "/etc/whitelist"
                  name: "ldap-sync-volume-whitelist"
                - mountPath: "/etc/secrets"
                  name: "ldap-bind-cert"
          volumes:
            - name: "ldap-sync-volume"
              configMap:
                name: "ldap-group-syncer"
            - name: "ldap-sync-volume-whitelist"
              configMap:
                name: "ldap-group-syncer-whitelist"
            - name: "ldap-bind-cert"
              secret:
                secretName: "ldap-bind-cert"
          restartPolicy: "Never"
          terminationGracePeriodSeconds: 30
          activeDeadlineSeconds: 500
          dnsPolicy: "ClusterFirst"
          serviceAccountName: "ldap-group-syncer"
          serviceAccount: "ldap-group-syncer"
          ```
