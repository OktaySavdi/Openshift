## Openshift API

```ruby
SERVER=`oc whoami --show-server`
TOKEN=`oc whoami --show-token`

URL="$SERVER/oapi/v1/users/~"

# Send a `GET` request to the Openshift API using `curl`:
curl -X GET -H "Authorization: Bearer $TOKEN" $URL --insecure

# Send a `GET` request to list all pods in the environment:
curl -X GET -H "Authorization: Bearer $TOKEN" $SERVER/api/v1/pods --insecure

# Send a `GET` request to the Openshift API using `curl`:
curl -X GET $SERVER

# Send a `GET` request to list all pods in the environment:
curl -X GET $SERVER/api/v1/pods

# Use `jq` to parse the json response:
curl -X GET $SERVER/api/v1/pods | jq .items[].metadata.name

# We can scope the response by only viewing all pods in a particular namespace:
curl -X GET $SERVER/api/v1/namespaces/myproject/pods

# Get more details on a particular pod within the `myproject` namespace:
curl -X GET $SERVER/api/v1/namespaces/myproject/pods/my-two-container-pod

# Patch the current pod with a newer container image (`1.15`):
curl -X PATCH $SERVER/api/v1/namespaces/myproject/pods/my-two-container-pod -H "Content-type: application/strategic-merge-patch+json" -d '{"spec":{"containers":[{"name": "server","image":"nginx:1.15-alpine"}]}}'

# Delete the current pod by sending the `DELETE` request method:
curl -X DELETE $SERVER/api/v1/namespaces/myproject/pods/my-two-container-pod

# Verify the pod no longer exists:
curl -X GET $SERVER/api/v1/namespaces/myproject/pods/my-two-container-pod

# The `oc scale` command interacts with the `/scale` endpoint:
curl -X GET $SERVER/apis/apps/v1/namespaces/myproject/replicasets/myfirstreplicaset/scale

# Use the `PUT` method against the `/scale` endpoint to change the number of replicas to 5
curl -X PUT $SERVER/apis/apps/v1/namespaces/myproject/replicasets/myfirstreplicaset/scale -H "Content-type: application/json" -d '{"kind":"Scale","apiVersion":"autoscaling/v1","metadata":{"name":"myfirstreplicaset","namespace":"myproject"},"spec":{"replicas":5}}'

# You can also get information regarding the pod by using the `GET` method against the `/status` endpoint
curl -X GET $SERVER/apis/apps/v1/namespaces/myproject/replicasets/myfirstreplicaset/status
```
