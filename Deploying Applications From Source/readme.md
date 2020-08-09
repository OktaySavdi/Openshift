## Deploying Applications From Source in Openshift 

**A new project is created**
```ruby
oc new-project upy
```
Login to the openshift console

![image](https://user-images.githubusercontent.com/3519706/89736602-3431e200-da73-11ea-8f52-028c053a6214.png)

Developer > +Add

![image](https://user-images.githubusercontent.com/3519706/89737199-cb993400-da77-11ea-84ed-620e508b9b22.png)

The most suitable template to be installed is selected. (Note: We will install Python for example purposes)

![image](https://user-images.githubusercontent.com/3519706/89737210-eb305c80-da77-11ea-9395-347e24a2653b.png)

Create Application

![image](https://user-images.githubusercontent.com/3519706/89737224-14e98380-da78-11ea-8bc1-5449276cd99f.png)

We will compile our code with python 3.6 below.

![image](https://user-images.githubusercontent.com/3519706/89737239-30ed2500-da78-11ea-9f56-2f1fec1b2e60.png)

Controls are made.

POD control is done via Developer> Topology

![image](https://user-images.githubusercontent.com/3519706/89737254-4b270300-da78-11ea-9513-adf6c8b02185.png)

Administrator> Builds> Image Streams It is checked that the image is created.

![image](https://user-images.githubusercontent.com/3519706/89737290-73aefd00-da78-11ea-8cb1-76fad3b0a6d7.png)

Check Build Logs

With Administrator> Builds> Builds you can view the logs in the build.

![image](https://user-images.githubusercontent.com/3519706/89737302-8cb7ae00-da78-11ea-9243-858e2bddcf4e.png)

![image](https://user-images.githubusercontent.com/3519706/89737307-9b05ca00-da78-11ea-9b83-13324f65e8dc.png)

All components are controlled via the CLI

![image](https://user-images.githubusercontent.com/3519706/89737318-b375e480-da78-11ea-822d-4d3f2b19db09.png)

**Deploying Using the Command Line**

**For the latest version of Python provided by the platform, we will distribute it using S2I**
```ruby
oc new-app python:latest~https://github.com/openshift-katacoda/blog-django-py
```
**Build process logs can be monitored**
```
oc logs bc/blog-django-py --follow
```
**The status of the deployed application is checked**
```
oc status
```
**Service is created for the created Deployment**
```
oc expose service/blog-django-py
```
**Creating a route for access from outside**
```
oc get route/blog-django-py
```
