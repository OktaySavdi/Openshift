## Setting Quotas Constraints

A resource quota, defined by a ResourceQuota object, provides constraints that limit aggregate resource consumption per project. It can limit the quantity of objects that can be created in a project by type, as well as the total amount of compute resources that may be consumed by resources in that project.

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: object-counts
spec:
  hard:
    pods: "3" 
    configmaps: "10" 
    persistentvolumeclaims: "4" 
    replicationcontrollers: "6" 
    secrets: "10" 
    services: "10"
    silver.storageclass.storage.k8s.io/requests.storage: "20Gi"
    silver.storageclass.storage.k8s.io/persistentvolumeclaims: "5"
    bronze.storageclass.storage.k8s.io/requests.storage: "0"
```

**CLI**
```ruby
oc create quota <name> \

--hard=count/<resource>.<group>=<quota>,count/<resource>.<group>=<quota>
oc create quota test \
--hard=count/deployments.extensions=2,count/replicasets.extensions=4,count/pods=3,count/secrets=4
```
```ruby
oc create -f quotas.yaml

oc get resourcequotas

oc describe resourcequotas object-counts
```
![image](https://user-images.githubusercontent.com/3519706/90726085-d353a680-e2c9-11ea-9d2d-9ae3afb13f62.png)

Let's deploy a new application
```ruby
oc run map --image=quay.io/oktaysavdi/map --restart=Always --port=8080
```
![image](https://user-images.githubusercontent.com/3519706/90726317-32192000-e2ca-11ea-8472-2d11b303c249.png)

Let's increase the number of pods to 3
```ruby
oc scale dc map --replicas=3
```
Since the 2nd pod quota is determined, the 3rd pod is not allowed to be created

![image](https://user-images.githubusercontent.com/3519706/90726459-6bea2680-e2ca-11ea-8e45-c617988419f2.png)
