## Copying Files to a Persistent Volume
If you are mounting a persistent volume into the container for your application and you need to copy files into it, then oc rsync can be used in the same way as described previously to upload files. All you need to do is supply as the target directory, the path of where the persistent volume is mounted in the container.

If you haven't as yet deployed your application, but are wanting to prepare in advance a persistent volume with all the data it needs to contain, you can still claim a persistent volume and upload the data to it. In order to do this, you will though need to deploy a dummy application against which the persistent volume can be mounted.

To create a dummy application for this purpose run the command:
```ruby
oc run nginx --image=nginx --restart=Always --labels="web=nginx"
```
We use the oc run command as it creates just a deployment configuration and managed pod. A service is not created as we don't actually need the application we are running here, an instance of the Apache HTTPD server in this case, to actually be contactable. We are using the Apache HTTPD server purely as a means of keeping the pod running.

To monitor the startup of the pod and ensure it is deployed, run:
```ruby
oc rollout status dc/nginx
```
Once it is running, you can see the more limited set of resources created, as compared to what would be created when using oc new-app, by running:
```ruby
oc get all --selector web=nginx -o name
```
Now that we have a running application, we next need to claim a persistent volume and mount it against our dummy application. When doing this we assign it a claim name of data so we can refer to the claim by a set name later on. We mount the persistent volume at /data inside of the container, the traditional directory used in Linux systems for temporarily mounting a volume.
```ruby
oc set volume po/nginx --add --name=tmp-mount --claim-name=data --type pvc --claim-size=1G --mount-path /data
```
This will cause a new deployment of our dummy application, this time with the persistent volume mounted. Again monitor the progress of the deployment so we know when it is complete, by running:
```ruby
oc rollout status dc/nginx
```
To confirm that the persistent volume claim was successful, you can run:
```ruby
oc get pvc
```
The command we will run from the shell function to get out just the name of the pod will be:
```ruby
POD=$(oc get pods --selector web=nginx -o jsonpath='{.items[?(@.status.phase=="Running")].metadata.name}')
```
We can now copy any files into the persistent volume, using the /mnt directory where we mounted the persistent volume, as the target directory. In this case since we are doing a one off copy, we can use the tar strategy instead of the rsync strategy.
```ruby
oc rsync ./ $POD:/data --strategy=tar
```
When complete, you can validate that the files were transferred by listing the contents of the target directory inside of the container.
```ruby
oc rsh $POD ls -las /data
```
