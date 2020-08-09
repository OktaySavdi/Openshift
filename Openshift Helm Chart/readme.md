## Openshift Helm Chart


**Helm Chart is created**
```ruby
helm create my-chart
```
**Chart is controlled**
```ruby
tree my-chart
```
![image](https://user-images.githubusercontent.com/3519706/89737423-8d9d0f80-da79-11ea-939d-a5153b8d10a4.png)

**The created chart is set up**
```ruby
helm install my-chart ./my-chart
```

**POD is checked**
```ruby
oc get pods
```
**The loaded chart is checked**
```ruby
helm ls
```
![image](https://user-images.githubusercontent.com/3519706/89737461-d0f77e00-da79-11ea-9516-05f281d1fab6.png)

**For Helm chart Upgrade**
```ruby
helm upgrade my-chart ./my-chart
```
**Helm chart upgrade and update existing configuration**
```ruby
helm upgrade my-chart ./my-chart --set image.pullPolicy = Always
```
**The change is controlled.**
```ruby
oc get deployment my-chart -o yaml | grep imagePullPolicy
```
**Version can be switched with Rollback**
```ruby
helm rollback my-chart 2 --dry-run
```
![image](https://user-images.githubusercontent.com/3519706/89737512-1156fc00-da7a-11ea-81ad-e660615a0721.png)

**Helm chart deletion**
```ruby
helm uninstall my-chart
```
