### Adding roles ;

| Default Cluster Role |  Description|
|--|--|
|**admin**| A project manager. If used in a local binding, an admin has rights to view any resource in the project and modify any resource in the project except for quota. |
|**basic-user**| A user that can get basic information about projects and users. |
|**cluster-admin**| A super-user that can perform any action in any project. When bound to a user with a local binding, they have full control over quota and every action on every resource in the project. |
|**cluster-status**| A user that can get basic cluster status information. |
|**edit**| A user that can modify most objects in a project but does not have the power to view or modify roles or bindings.|
|**self-provisioner**|A user that can create their own projects.|
|**view**| A user who cannot make any modifications, but can see most objects in a project. They cannot view or modify roles or bindings. |

 **1. Add a role to a user in a specific project:**
```
oc adm policy add-role-to-user <role> <user> -n <project>
```
**2. Add a role to a group**
```
oc adm policy add-role-to-group <role> <group> -n <project>
```
 **3. View the local role bindings and verify the addition in the output:**
```
oc describe rolebinding.rbac -n <project>
```
**4. Creating a cluster admin**
```
oc adm policy add-cluster-role-to-user cluster-admin <user>
```
**5. Add cluster-admin to a serviceaccount**
```
oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:default:<my_sa>
```
