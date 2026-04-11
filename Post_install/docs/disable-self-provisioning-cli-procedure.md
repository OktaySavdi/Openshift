# Disable Self-Provisioning - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- Understanding of RBAC implications

## Overview
Disable self-provisioning so regular users cannot create projects; enforce centralized project creation.

## Step 1: Remove Self-Provisioner Role
```bash
oc describe clusterrolebinding self-provisioners
oc patch clusterrolebinding self-provisioners -p '{"subjects": []}'
# Or
oc delete clusterrolebinding self-provisioners
```

## Step 2: Prevent Auto-Creation
```bash
oc patch clusterrolebinding self-provisioners -p '{"metadata":{"annotations":{"rbac.authorization.kubernetes.io/autoupdate":"false"}}}'
```

## Step 3: Custom Project Request Template (Optional)
```yaml
apiVersion: template.openshift.io/v1
kind: Template
metadata:
  name: project-request
  namespace: openshift-config
objects:
- apiVersion: project.openshift.io/v1
  kind: Project
  metadata:
    annotations:
      openshift.io/description: ${PROJECT_DESCRIPTION}
      openshift.io/display-name: ${PROJECT_DISPLAYNAME}
      openshift.io/requester: ${PROJECT_REQUESTING_USER}
    name: ${PROJECT_NAME}
  spec: {}
- apiVersion: rbac.authorization.k8s.io/v1
  kind: RoleBinding
  metadata:
    name: admin
    namespace: ${PROJECT_NAME}
  roleRef:
    apiGroup: rbac.authorization.k8s.io
    kind: ClusterRole
    name: admin
  subjects:
  - apiGroup: rbac.authorization.k8s.io
    kind: User
    name: ${PROJECT_ADMIN_USER}
parameters:
- name: PROJECT_NAME
- name: PROJECT_DISPLAYNAME
- name: PROJECT_DESCRIPTION
- name: PROJECT_ADMIN_USER
- name: PROJECT_REQUESTING_USER
```
```bash
oc apply -f project-request-template.yaml
oc edit project.config.openshift.io/cluster  # add spec.projectRequestTemplate.name
```

## Step 4: Create Admin Group
```bash
oc adm groups new project-admins
oc adm groups add-users project-admins admin1 admin2
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: project-admins-self-provisioner
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: self-provisioner
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: project-admins
EOF
```

## Verification
```bash
oc describe clusterrolebinding self-provisioners
oc login -u testuser
oc new-project test-project  # should fail
oc login -u admin1
oc new-project test-project  # should succeed
oc get template project-request -n openshift-config
```

## Troubleshooting
```bash
oc get clusterrolebinding | grep self-provisioner
oc get clusterrolebinding -o json | jq '.items[] | select(.roleRef.name=="self-provisioner")'
oc get project.config.openshift.io/cluster -o yaml
oc auth can-i create projects
oc auth can-i create projects --as=testuser
```

## Re-Enable Self-Provisioning
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: self-provisioners
  annotations:
    rbac.authorization.kubernetes.io/autoupdate: "true"
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: self-provisioner
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: system:authenticated:oauth
```
```bash
oc apply -f self-provisioners.yaml
# Or patch
oc patch clusterrolebinding self-provisioners -p '{"subjects":[{"apiGroup":"rbac.authorization.k8s.io","kind":"Group","name":"system:authenticated:oauth"}]}'
```

## Alternative: Project Request Message
```bash
oc edit project.config.openshift.io/cluster
# Add:
spec:
  projectRequestMessage: "Please contact platform@example.com to request a new project"
```

## Configuration Options
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ldap-project-admins
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: self-provisioner
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: CN=OpenShift-Admins,OU=Groups,DC=example,DC=com
```
Resource quota / network policy examples can be embedded in template.

## Best Practices
- Use groups for delegation
- Add quotas & policies by default
- Custom request message for UI
- Audit existing projects regularly
- Naming conventions enforced externally
- Integrate with ticketing system

## Notes
- Cluster admins unaffected
- Existing projects remain
- Users retain access to granted projects
- Template optional
- Changes immediate
