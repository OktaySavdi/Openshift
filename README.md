## Openshift CLI Commands

**CLI Example**
```ruby
oc run nginx --image nginx --replicas=1 \
                  --port='8080' --requests 'memory=15Mi' \
                  --limits 'memory=20Mi' --restart Never \
                  --labels='app=nginx' --dry-run -o yaml \
                  --command -- sleep 1000
```
**ConfigMap**
```ruby
oc create configmap webapp-config-map
# file
oc create configmap myconfig --from-file=example-files/game.properties --from-file=example-files/ui.properties

# Value
oc create configmap myconfig --from-literal=special.how=very --from-literal=special.type=charm

# Volume
oc set volume dc/map --add --name=v1 --type=configmap --configmap-name='myconfig' --mount-path=/data

oc set volume dc/<DC-NAME> -t configmap --name trusted-ca --add --read-only=true --mount-path /etc/pki/ca-trust/extracted/pem --configmap-name <CONFIGMAP-NAME>

# Env
oc set env --from=configmap/deneme dc/map
```
**secret**
```ruby
oc create secret generic db-secret \
        --from-literal=DB_HOST=sql01 \
        --from-literal=DB_User=root \
        --from-literal=DB_Password=password123 
```
```ruby
oc create secret docker-registry private-reg-cred \
               --docker-username=dock_user \
               --docker-password=dock_password \
               --docker-server=myprivateregistry.com:5000 \
               --docker-email=dock_user@myprivateregistry.com
```
```ruby
# Generic
oc create secret generic test-secret --from-literal=username='my-app' --from-literal=password='39528$vdg7Jb'

# Env
oc set env --from=secret/test-secret dc/map

# Volume
oc set volume rc/r1 --add --name=v1 --type=secret --secret-name='secret1' --mount-path=/data
```
**Limit - Resource**
```ruby
oc set resources deployment hello-world-nginx --requests cpu=10m,memory=20Mi --limits cpu=80m,memory=100Mi
```
**Probe**
```ruby
oc set probe deployment/hello-node --readiness --get-url=http://:8766/actuator/health --timeout-seconds=1 --initial-delay-seconds=15 --liveness --get-url=http://:8766/actuator/health --timeout-seconds=1 --initial-delay-seconds=15
```
**Create and add a Persistent Volume**
```
oc set volume dc/name-of-your-app-here --add --name=storage --type='persistentVolumeClaim' --claim-class='standard' --claim-name='storage' --claim-size='10Gi' --mount-path=/var/www/html

oc set volume -f dc.json --add --name=v1 --type=persistentVolumeClaim --claim-name=pvc1 --mount-path=/data --containers=c1
```
**Token**
```ruby
oc create serviceaccount robot
oc policy add-role-to-user admin system:serviceaccount:test:robot
oc serviceaccounts get-token robot

SERVER=`oc whoami --show-server`
TOKEN=`oc whoami --show-token`

URL="$SERVER/oapi/v1/users/~"

curl -H "Authorization: Bearer $TOKEN" $URL --insecure
```

**Rollout-Rollback**
```ruby
oc rollout latest dc/example
oc rollout status deployment example --timeout 90s
oc set image deployment/example MyContainerName=quay.io/oktaysavdi/istioproject:v2
oc rollout history deployment example
oc rollout undo deployment example
oc rollout history deployment example --revision=2
oc rollout undo deployment example  --to-revision=3
```
**skopeo**
```ruby
skopeo copy docker://quay.io/redhattraining/versioned-hello:v1.1 docker://quay.io/oktaysavdi/versioned-hello:latest
```

**Cheat Sheet**
```ruby
# Use the --v flag to set a verbosity level.
oc get pods --v=8

# Delete Evicted pods
oc get pod  | grep Evicted | awk '{print $1}' | xargs oc delete pod

# Delete Failed pods
oc delete $(oc get pods --field-selector=status.phase=Failed -o name -n cluster-management) -n cluster-management

# Auto scale
oc autoscale dc/hello --min 1 --max 10 --cpu-percent 80

oc config view
oc config get-contexts                           # display list of contexts 
oc config current-context                        # display the current-context
oc config use-context my-cluster-name            # set the default context to my-cluster-name
oc config set-context --current --namespace=MyNS # permanently save the namespace for all subsequent oc commands in that context.

# Get commands with basic output
oc get services                          # List all services in the namespace
oc get pods --all-namespaces             # List all pods in all namespaces
oc get pods -o wide                      # List all pods in the current namespace, with more details
oc get deployment my-dep                 # List a particular deployment
oc get pods                              # List all pods in the namespace
oc get pod my-pod -o yaml                # Get a pod's YAML

# List Services Sorted by Name
oc get services --sort-by=.metadata.name

# List PersistentVolumes sorted by capacity
oc get pv --sort-by=.spec.capacity.storage

# add env to nginx-app
oc set env deployment/nginx-app  DOMAIN=cluster

# Get all worker nodes 
oc get node --selector='!node-role.kubernetes.io/master'

# list container images
oc get pods -A -o jsonpath='{.items[*].spec.containers[*].image}' 

# List pods Sorted by Restart Count
oc get pods --sort-by='.status.containerStatuses[0].restartCount'

# Get the version label of all pods with label app=cassandra
oc get pods --selector=app=cassandra -o jsonpath='{.items[*].metadata.labels.version}'

# Get all running pods in the namespace
oc get pods --field-selector=status.phase=Running

# get annotations in pods
oc get deployment/hello -o json |  jq '.metadata.annotations'

oc get pod --all-namespaces -o=custom-columns=NAME:.metadata.name,STATUS:.status.phase,NODE:.spec.nodeName
oc get pod -o=custom-columns=NAME:.metadata.name,STATUS:.status.phase,NODE:.spec.nodeName,NAMESPACE:.metadata.namespace --all-namespaces

oc get pods -A -o jsonpath='{range .items[*]}' '{.metadata.namespace} {.metadata.creationTimestamp}{"\n"}'

oc get -A pods -o jsonpath='{range.items[*]}''{"\n"}{.metadata.namespace}{"\t"}{.metadata.name}{"\t"}''{range.status.conditions[*]}{.type}-{.status},{.end}{end}{"\n"}'

# Get capacity storage
oc get pv --sort-by=.spec.capacity.storage
oc get pv --sort-by=.spec.capacity.storage -o=custom-columns=NAME:.metadata.name,CAPACITY:.spec.capacity.storage 

# Partially update a node
oc patch node k8s-node-1 -p '{"spec":{"unschedulable":true}}'

# Update a node env
oc patch deployment/myapp --patch '{"spec":{"template":{"spec":{"nodeSelector":{"env":"dev"}}}}}'

# Add a new element to a positional array
oc patch sa default --type='json' -p='[{"op": "add", "path": "/secrets/1", "value": {"name": "whatever" } }]'

# Update a container's image; spec.containers[*].name is required because it's a merge key
oc patch pod valid-pod -p '{"spec":{"containers":[{"name":"kubernetes-serve-hostname","image":"new image"}]}}'

# change pod image version
oc patch dc nginx --patch='{"spec":{"template":{"spec":{"containers":[{"name": "nginx", "image":"nginx:1.19.1"}]}}}}'

# Update a container's image using a json patch with positional arrays
oc patch pod valid-pod --type='json' -p='[{"op": "replace", "path": "/spec/containers/0/image", "value":"new image"}]'

# Disable a deployment livenessProbe using a json patch with positional arrays
oc patch deployment valid-deployment --type json -p='[{"op": "remove", "path": "/spec/template/spec/containers/0/livenessProbe"}]'

# expose a port through with a service
oc expose deployment nginx-app --port=80 --name=nginx-http

# login inside pod
oc exec -ti nginx-app-5jyvm -- /bin/sh

# Scale pods
oc scale replicaset myfirstreplicaset --replicas=3
```
**jsonpath**
```ruby
oc config view -o=jsonpath='{.users[*].name}'

oc get nodes node01 -o jsonpath='{.metadata.name}'

oc get nodes -o jsonpath='{.items[*].metadata.name}'

oc get deployment -o jsonpath='{.status.availableReplicas}'

oc get route -n openshift-monitoring -o jsonpath='{.items[*].spec.host}'

kuconfig view --kubeconfig=my-kube-config -o jsonpath="{.users[*].name}"

oc get nodes -o jsonpath='{.items[*].status.addresses[?(@.type=="InternalIP")].address}'

oc get secret elasticsearch-es-elastic-user -o=jsonpath='{.data.elastic}' | base64 --decode

oc config view --kubeconfig=my-kube-config -o jsonpath="{.contexts[?(@.context.user=='aws-user')].name}"

oc get deployment -n openshift-cluster-samples-operator cluster-samples-operator -o jsonpath='{.status.conditions[*].type}'

oc get deployment -n openshift-cluster-samples-operator cluster-samples-operator -o jsonpath='{.spec.template.spec.containers[0].name}'

oc get deployment -n openshift-cluster-samples-operator cluster-samples-operator -o jsonpath='{.status.conditions[?(@.type=="Available")].status}'
```
**Cluster**
```ruby
oc config view
oc cluster-info                                                  # Display addresses of the master and services
oc cluster-info dump                                             # Dump current cluster state to stdout
oc cluster-info dump --output-directory=/path/to/cluster-state   # Dump current cluster state to /path/to/cluster-state
```
**Template**
```ruby
oc get template -n openshift | grep jenkins
oc process jenkins-persistent --parameters -n openshift
oc new-app --template jenkins-persistent
```
**Taint - Toleration**
```ruby
oc taint nodes node-name key=value:taint-effect
oc taint nodes node-name app=blue:NoSchedule
```
```yaml
tolerations:
- key: "app"
  operator: "Equal"
  value: "blue"
  effect: "NoSchedule"
```
```ruby
oc taint nodes node1 key1=value1:NoSchedule
oc taint nodes node1 key1=value1:NoExecute
oc taint nodes node1 key2=value2:NoSchedule
```
**Example**
```
oc taint nodes node01 spray=mortein:NoSchedule
```
```yaml
tolerations:
- key: "spray"
  operator: "Equal"
  value: "mortein"
  effect: "NoSchedule"
```
**delete taint**
```ruby
oc taint node master node-role.kubernetes.io/master:NoSchedule-
```
**Node Selector**
```ruby
oc label nodes node-1 size=Large
```
```yaml
nodeSelector:
 size: Large
```

**Affinity**
**Available**
```
requiredDuringSchedulingIgnoredDuringExecution # does not assign if there is no matching node
```
```
preferredDuringSchedulingIgnoredDuringExecution # if there is no matching node, it makes an assignment
```
**Planning**
```
requiredDuringSchedulingRequiredDuringExecution
```
```yaml
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        - key: color
          operator: In
          values:
          - blue
```
```yaml
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        - key: node-role.kubernetes.io/master
          operator: Exists
```



**Static Pod**

```ruby
oc run --restart=Never --image=busybox:1.28.4 \
            static-busybox --dry-run -o yaml \ 
            --command -- sleep 1000 > /etc/kubernetes/manifests/static-busybox.yaml
```
**Upgrade**
```ruby
oc drain node01 --ignore-daemonsets --force  > takes maintenance mode. deletes every pod on it
```
```ruby
oc uncordon node01 > reactivates the node from maintenance mode. starts pod on it
```
```ruby
oc cordon node01  > run existing ones but not new pod
```
**Triggers**
```ruby
oc import-image quay.io/oktaysavdi/versioned-hello:latest --confirm --scheduled

oc set triggers deployment/versioned-hello --from-image versioned-hello:latest -c hello
```
**Backup**
```ruby
ETCDCTL_API=3 etcdctl snapshot save \
/tmp/snapshot-pre-boot.db \
--endpoints=https://[127.0.0.1]:2379 \
--cacert=/etc/kubernetes/pki/etcd/ca.crt \
--cert=/etc/kubernetes/pki/etcd/server.crt \
--key=/etc/kubernetes/pki/etcd/server.key
```
```ruby
ETCDCTL_API=3 etcdctl member list \
--endpoints=https://[127.0.0.1]:2379 \
--cacert=/etc/kubernetes/pki/etcd/ca.crt \
--cert=/etc/kubernetes/pki/etcd/server.crt \
--key=/etc/kubernetes/pki/etcd/server.key
```
**Restore**
```ruby
ETCDCTL_API=3 etcdctl snapshot restore \
/tmp/snapshot-pre-boot.db \
--endpoints=https://[127.0.0.1]:2379 \
--cacert=/etc/kubernetes/pki/etcd/ca.crt \
--name=master --cert=/etc/kubernetes/pki/etcd/server.crt \
--key=/etc/kubernetes/pki/etcd/server.key \
--data-dir /var/lib/etcd-from-backup \
--initial-cluster=master=https://127.0.0.1:2380 \
--initial-cluster-token etcd-cluster-1 \
--initial-advertise-peer-urls=https://127.0.0.1:2380
```
**Certificate**
```ruby
oc get csr 

oc certificate approve <cert-name>

oc certificate deny <cert-name>

oc delete csr <cert-name>
```
**RBAC**
```ruby
oc get roles --all-namespaces

oc get roles weave-net -n kube-system -o yaml

oc describe rolebindings weave-net -n kube-system
```
**Security Context**
```yaml
spec:
  securityContext:
    runAsUser: 1000
    runAsGroup: 3000
    fsGroup: 2000
```
## DUMP

**TCP Dump**
```ruby
nsenter -t 5436 -n -- tcpdump -i any --nn -w /host/tmp/(pod_name).pcap
```
**Memory Dump**
```
jcmd 1 GC.heap_dump /tmp/heap.hprof
```
```
$ oc exec tomcat8-4-37683 -c tomcat8 -- jcmd 1 GC.class_histogram
```
```
$ oc exec tomcat8-4-37683 -c tomcat8 -- jcmd 1 GC.heap_dump /tmp/heap.hprof \
oc rsync tomcat8-4-37683:/tmp/heap.hprof . \
oc exec tomcat8-4-37683 -- rm /tmp/heap.hprof
```

**Thread Dump**
```
jcmd 1 Thread.print
```
```
kill -3 1
```
```
oc exec tomcat8-4-37683 -c tomcat8 -- jcmd 1 Thread.print
```
```
oc exec tomcat8-4-37683 -c tomcat8 -- kill -3 1
```
**Garbage collections**
```
jstat -gcutil 1 10000 5 > jstat.out
```
```
oc exec tomcat8-4-37683 -- jstat -gcutil 1 10000 5 > jstat.out
```
