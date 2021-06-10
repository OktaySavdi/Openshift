## Downloading Files from a Container

To create a dummy application for this purpose run the command:
```ruby
oc run nginx --image=nginx --restart=Always --labels="web=nginx"
```
The command we will run from the shell function to get out just the name of the pod will be:
```ruby
POD=$(oc get pods --selector deploymentconfig=blog -o jsonpath='{.items[?(@.status.phase=="Running")].metadata.name}')
```
To copy files from the local machine to the container, the oc rsync command is again used.
The form of the command when copying files from the local machine to the container is:
```ruby
oc rsync ./local/dir <pod-name>:/remote/dir
```
Unlike when copying from the container to the local machine, there is no form for copying a single file. To copy selected files only, you will need to use the --exclude and --include options to filter what is and isn't copied from a specified directory.

For the web application being used, it hosts static files out of the nginx sub directory of the application source code. To upload the nginx.conf file run:
```ruby
oc rsync . $POD:/oktay --no-perms
```
```ruby
oc rsync . $POD:/oktay --exclude=* --include=nginx.conf --no-perms
```
```ruby
oc rsh $POD
```
```ruby
ls /oktay
```
