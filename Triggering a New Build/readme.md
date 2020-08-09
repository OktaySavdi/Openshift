**Triggering a New Build in Openshift**

To do it from the command line, run the following command:
```ruby
oc start-build blog-django-py
```
You can use oc logs to monitor the log output while the build is running. You can also track the progress of any build in a project by running the command:
```ruby
oc get builds --watch
```
![image](https://user-images.githubusercontent.com/3519706/89737576-888c9000-da7a-11ea-8d7f-47b0d86fffe5.png)

You can run the application to view information about the build configuration:
```ruby
oc describe bc / blog-django-py
```
The following command is run to build a local file
```ruby
oc start-build blog-django-py --from-dir =. --wait
```
