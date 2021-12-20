
# Pull an Image from a Private Registry in Openshift

**Create a Secret by providing credentials on the command line**

Create this Secret, naming it `regcred`:

```ruby
oc create secret docker-registry regcred --docker-server=<your-registry-server> --docker-username=<your-name> --docker-password=<your-pword> --docker-email=<your-email>
```

where:

-  `<your-registry-server>`  is your Private Docker Registry FQDN. 

-  `<your-name>`  is your Docker username.

-  `<your-pword>`  is your Docker password.

-  `<your-email>`  is your Docker email.

You have successfully set your Docker credentials in the cluster as a Secret called  `regcred`.

## Inspecting the Secret  `regcred`

To understand the contents of the  `regcred`  Secret you just created, start by viewing the Secret in YAML format:

```ruby
oc get secret regcred --output=yaml
```
The output is similar to this:
```yaml
apiVersion: v1
kind: Secret
metadata:
  ...
  name: regcred
  ...
data:
  .dockerconfigjson: eyJodHRwczovL2luZGV4L ... J0QUl6RTIifX0=
type: kubernetes.io/dockerconfigjson
```

The value of the  `.dockerconfigjson`  field is a base64 representation of your Docker credentials.

To understand what is in the  `.dockerconfigjson`  field, convert the secret data to a readable format:

```ruby
oc get secret regcred --output="jsonpath={.data.\.dockerconfigjson}" | base64 --decode
```
To understand what is in the `auth` field, convert the base64-encoded data to a readable format:
```ruby
echo "c3R...zE2" | base64 --decode
```
**Create a Pod that uses your Secret**
pods/private-reg-pod.yaml 
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: private-reg
spec:
  containers:
  - name: private-reg-container
    image: <your-private-image>
  imagePullSecrets:
  - name: regcred
```
or

Another way of creating the secret is to use the authentication token from the podman login
command:
```
[user@host ~]$ oc create secret generic registrycreds \
--from-file .dockerconfigjson=${XDG_RUNTIME_DIR}/containers/auth.json \
--type kubernetes.io/dockerconfigjson
```
You then link the secret to the default service account from your project:
```
[user@host ~]$ oc secrets link default registrycreds --for pull
```
To use the secret to access an S2I builder image, link the secret to the builder service account
from your project:
```
[user@host ~]$ oc secrets link builder registrycreds
```
