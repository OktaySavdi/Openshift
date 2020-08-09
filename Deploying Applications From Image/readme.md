## Deploying Applications From Image in Openshift

**A new project is created**
```ruby
oc new-project upy
```
Login to the openshift console
![image](https://user-images.githubusercontent.com/3519706/89736602-3431e200-da73-11ea-8f52-028c053a6214.png)

Developer > +Add

![image](https://user-images.githubusercontent.com/3519706/89736638-68a59e00-da73-11ea-898b-700ee3751e25.png)

![image](https://user-images.githubusercontent.com/3519706/89736668-ba4e2880-da73-11ea-9be4-6975e541c711.png)

Controls are made.
POD control is done via Developer> Topology

![image](https://user-images.githubusercontent.com/3519706/89736686-d9e55100-da73-11ea-95ee-2943859173e5.png)

Administrator> Builds> Image Streams It is checked that the image is created.

![image](https://user-images.githubusercontent.com/3519706/89736702-faada680-da73-11ea-8761-2c015be871ec.png)

All components are controlled via the CLI
```ruby
oc get all -o name
```
![image](https://user-images.githubusercontent.com/3519706/89736728-1dd85600-da74-11ea-925a-cda4d64e055f.png)
