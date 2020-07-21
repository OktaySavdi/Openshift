## Downloading Files from a Container

To create a dummy application for this purpose run the command:
```ruby
oc run nginx --image=nginx --restart=Always --labels="web=nginx"
```
The command we will run from the shell function to get out just the name of the pod will be:
```ruby
POD=$(oc get pods --selector web=nginx -o jsonpath='{.items[?(@.status.phase=="Running")].metadata.name}')
```
To create an interactive shell within the same container running the application, you can use the oc rsh command, supplying it the environment variable holding the name of the pod.
```ruby
oc rsh $POD
```
From within the interactive shell, see what files exist in the application directory.
```bash
ls -las
```
To confirm what directory the file is located in, inside of the container, run:
```bash
pwd
```
To exit the interactive shell and return to the local machine run:
```bash
exit
```
To copy files from the container to the local machine the oc rsync command can be used.

The form of the command when copying a single file from the container to the local machine is:
```ruby
oc rsync <pod-name>:/remote/dir/filename ./local/dir
```
To copy the single file run:
```ruby
oc rsync $POD:/etc/nginx/nginx.conf .
```
This should display output similar to:
receiving incremental file list
```ruby
nginx.conf
```
sent 43 bytes  received 44,129 bytes  88,344.00 bytes/sec
total size is 44,032  speedup is 1.00
Check the contents of the current directory by running:
```ruby
ls -las
```
In addition to copying a single file, a directory can also be copied. The form of the command when copying a directory to the local machine is:
```ruby
oc rsync <pod-name>:/remote/dir ./local/dir
```
To copy the media directory from the container, run:
```ruby
oc rsync $POD:/etc/nginx/ .
```
If you wanted to rename the directory when it is being copied, you should create the target directory with the name you want to use first.
```ruby
mkdir uploads
```
and then to copy the files use the command:
```ruby
oc rsync $POD:/etc/nginx/. uploads
```
