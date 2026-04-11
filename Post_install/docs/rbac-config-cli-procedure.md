# RBAC CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- User/group identities configured (LDAP, OAuth, etc.)

## OVERVIEW
Configure Role-Based Access Control (RBAC) to manage user permissions and access to cluster resources.

## STEP 1: VIEW EXISTING ROLES
```bash
oc get clusterroles
oc get clusterroles | grep -E '^(admin|edit|view|cluster-admin)'
oc describe clusterrole admin
oc get rolebindings -n <namespace>
oc get clusterrolebindings
```

## STEP 2: GRANT CLUSTER ADMIN
```bash
oc adm policy add-cluster-role-to-user cluster-admin admin-user
oc adm policy add-cluster-role-to-group cluster-admin admins-group
oc adm policy remove-cluster-role-from-user cluster-admin admin-user
```

## STEP 3: GRANT PROJECT ADMIN
```bash
oc adm policy add-role-to-user admin developer1 -n myproject
oc adm policy add-role-to-group admin developers -n myproject
oc adm policy add-role-to-user admin user1 user2 user3 -n myproject
```

## STEP 4: GRANT VIEW/EDIT PERMISSIONS
```bash
oc adm policy add-role-to-user view viewer1 -n myproject
oc adm policy add-role-to-user edit developer1 -n myproject
oc adm policy add-cluster-role-to-user cluster-reader ops-team
```

## STEP 5: CREATE CUSTOM CLUSTERROLE
```yaml
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: pod-reader
rules:
- apiGroups: [""]
  resources: ["pods", "pods/log"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods/exec"]
  verbs: ["create"]
EOF
# Grant role
oc adm policy add-cluster-role-to-user pod-reader developer1
```

## STEP 6: CREATE CUSTOM NAMESPACE ROLE
```yaml
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: deployment-manager
  namespace: myproject
rules:
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: [""]
  resources: ["pods", "pods/log"]
  verbs: ["get", "list", "watch"]
EOF
# Bind
oc adm policy add-role-to-user deployment-manager developer1 -n myproject
```

## STEP 7: ROLEBINDING MANUAL EXAMPLE
```yaml
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: developers-admin
  namespace: myproject
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: admin
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: developers
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: developer1
EOF
```

## STEP 8: CLUSTERROLEBINDING EXAMPLE
```yaml
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ops-cluster-readers
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-reader
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: ops-team
EOF
```

## VERIFICATION
```bash
oc auth can-i create pods --as=developer1 -n myproject
oc auth can-i delete projects --as=developer1
oc auth can-i get nodes --as-group=ops-team
oc policy who-can delete pods -n myproject
oc get rolebindings,clusterrolebindings --all-namespaces -o wide | grep <username>
oc auth can-i --list -n myproject
```

## TROUBLESHOOTING
```bash
oc describe rolebinding <binding> -n <ns>
oc describe clusterrolebinding <binding>
oc auth can-i --list --as=<user> -n <ns>
oc describe clusterrole <role>
oc get groups; oc describe group <group>
```

## REMOVE PERMISSIONS
```bash
oc adm policy remove-role-from-user admin developer1 -n myproject
oc adm policy remove-cluster-role-from-user cluster-admin admin-user
oc delete rolebinding developers-admin -n myproject
oc delete clusterrolebinding ops-cluster-readers
```

## COMMON CLUSTERROLES
```text
cluster-admin: Full cluster access
admin: Full project access + modify RBAC
edit: Modify resources, not RBAC
view: Read-only
cluster-reader: Read-only cluster-wide
self-provisioner: Can create projects
registry-viewer/editor: Image pull/push
system:image-puller / system:deployer: Internal service accounts
```

## CUSTOM ROLE EXAMPLES
```yaml
# Secret reader
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: secret-reader
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get", "list"]
---
# Node manager
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: node-manager
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch", "update", "patch"]
- apiGroups: [""]
  resources: ["nodes/status"]
  verbs: ["patch"]
---
# Namespace admin
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: namespace-admin
  namespace: myproject
rules:
- apiGroups: ["*"]
  resources: ["*"]
  verbs: ["*"]
```

## SERVICE ACCOUNT RBAC
```bash
oc create sa myapp-sa -n myproject
oc adm policy add-role-to-user edit system:serviceaccount:myproject:myapp-sa -n myproject
oc adm policy add-cluster-role-to-user cluster-reader system:serviceaccount:myproject:myapp-sa
```
```yaml
# Pod spec excerpt
spec:
  serviceAccountName: myapp-sa
```

## BEST PRACTICES
- Prefer groups over individual users
- Least privilege principle
- Namespace-scoped Roles when possible
- Custom roles for precise control
- Document all custom roles
- Regular RBAC audits
- Use view/edit before admin
- Avoid cluster-admin for routine tasks
- ServiceAccounts for applications
- Test permissions prior to grant

## RBAC AUDIT COMMANDS
```bash
oc get clusterrolebindings -o wide
oc get clusterrolebindings -o json | jq -r '.items[] | select(.roleRef.name=="cluster-admin") | .subjects[]?.name'
oc get rolebindings -n myproject -o json | jq -r '.items[] | select(.roleRef.name=="admin") | .subjects[]?.name'
oc get clusterroles,clusterrolebindings,roles,rolebindings --all-namespaces -o yaml > rbac-backup.yaml
```

## NOTES
- RBAC changes apply immediately
- ClusterRoles are cluster-wide; Roles are namespace-scoped
- Bindings connect roles to subjects (users/groups/SAs)
- Default roles immutable (create custom instead)
- ServiceAccounts auto-token
- External groups synced from identity providers
