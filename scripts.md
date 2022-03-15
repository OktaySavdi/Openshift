### find the active master/etcd host
```sh
oc get cm kube-controller-manager -o yaml -n kube-system
```
### find ip addres in node
```sh
for i in $(oc get nodes -o wide | grep infra | awk '{print $6}'); do ssh core@$i 'sudo ip a | grep 10.10.10.10'; done
```
### Check cluster URL
```sh
oc config view --minify -o jsonpath='{.clusters[0].cluster.server}'
```
### Check stuations imajpullbackoff
```sh
oc get po -A | grep ImagePullBackOff | awk '{print $2}' | rev | cut -d "-" -f3- | rev | sort | uniq | wc -l
```
### Certificate expires date
```sh
oc get cm -n openshift-kube-apiserver-operator kube-apiserver-to-kubelet-client-ca -ojsonpath='{.data.ca-bundle\.crt}'
oc get cm -n openshift-kube-apiserver-operator kube-apiserver-to-kubelet-client-ca -ojsonpath='{.data.ca-bundle\.crt}'| openssl crl2pkcs7 -nocrl -certfile /dev/stdin | openssl pkcs7 -print_certs -text | egrep -A4 Issuer
```
### Certificate Approve
```sh
oc get csr -o name | xargs oc adm certificate approve
or
for i in $(oc get csr --no-headers | grep -i Pending | awk '{print $1}');do oc adm certificate approve $i;done
or
oc get csr -o json | jq -r '.items[] | select(.status == {}) | .metadata.name' | xargs oc adm certificate approve
```
### Certificate detail
```sh
oc get secret openshift-gitops-cluster -n openshift-gitops -o jsonpath='{.data.admin\.password}' | base64 -d
oc get secret my-cache-cluster-cert-secret -n oktay -o jsonpath='{.data.tls\.crt}' | base64 --decode
```
### Delete Evicted Pod
```sh
oc get po | grep Evicted | awk '{print $1}' | xargs oc delete po --grace-period=0 --force
oc get pod -A --no-headers | grep -v openshift | awk '{if ($4=="Failed" || $4=="Completed") print "oc delete pod " $2 " -n " $1;}' | sh 
```
### Delete InvalidHost Route
```sh
oc get route -A --no-headers | awk '{if ($3=="InvalidHost") print "oc delete route " $2 " -n " $1;}' | sh
```
### Delete HPA
```sh
oc get hpa -A --no-headers | awk '{print $1}' | sort | uniq > config
while read -r i ; do  for item in `oc get hpa --no-headers -n $i | awk '{print $1}' `; do  oc delete hpa $item -n ${i};     done; done < "config"
    ```
### Desirestate 0
```sh
for ns in $(oc get ns -o custom-columns=NAME:.metadata.name | grep -v NAME | grep dev | grep -v openshift); do oc -n ${ns} get dc -o custom-columns=NAME:.metadata.name,NS:.metadata.namespace,DESIRED:.status.replicas,AVAILABLE:.status.availableReplicas | grep 0;done
```
### Disable Selfprovisioner
```
oc annotate clusterrolebinding.rbac self-provisioner 'rbac.authorization.kubernetes.io/autoupdate=false' --overwrite
```
### Events list to time
```sh
oc get events --sort-by='.metadata.creationTimestamp'
```
### Get Token
```sh
oc sa get-token <my_serviceaccount>
TOKEN=$(oc get secret $(oc get sa <my_serviceaccount> -o jsonpath='{.secrets[0].name}') -o jsonpath='{.data.token}' | base64 --decode )
```
### KubeConfig
```sh
oc config set-context system-admin --cluster=api-mycluster.mydomain:6443 --user=admin
oc config get-contexts 
oc config use-context system-admin
```
### List DC
```sh
oc get projects --no-headers | grep -vE 'openshift|kube' | awk '{print $1}' > config
while read -r i;do for dc in $(oc get dc --no-headers -n $i | awk '{print $1}');do echo "$dc - $i";done;done < config >> list.txt
or
oc get projects --no-headers | grep -vE 'openshift|kube' | awk '{print $1}' > config
while read -r line;do oc get dc -o name -n $line;done < "config"
```
### List Pod
```sh
oc get project | grep -v openshift | grep -iE 'qa|test' | awk '{print $1}' > config
while read -r line;do oc get po -o name -n $line ;done < "config"
while read -r line;do if [[ $(oc get po -o name -n $line | wc -l) -gt 0 ]]; then echo "$line"; oc get po -o name -n $line;fi done < "config"
```
### List Resource-Limit
```sh
oc get dc -A -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.namespace}{"\t"}{..resources.requests}{"\n"}{end}'
or
oc get dc -A -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.namespace}{"\t"}{..resources.requests}{"\t"}{..resources.limits}{"\n"}{end}'
    ```
### List non version dc
```sh
oc get project --no-headers  | grep -v openshift | awk '{print $1}'  > config
while read -r line;do oc get dc -o name -n $line | grep -Ev "v1|v2|v3|v0|v4";done < "config"
```
### List detail pod
```sh
kubectl get pods -o custom-columns=RESTART:.metadata.name,RESTART:.status.containerStatuses[*].restartCount,STATUS:.status.phase,IP:.status.podIP,NODE:.spec.nodeName,IMAGE:.spec.containers[*].image,ContainerName:.spec.containers[*].name --sort-by=.metadata.creationTimestamp
```
### Node date
```bash
for  i in `oc get nodes --no-headers -o wide | awk '{print $6}'`; do ssh core@${i} date; done
```
### Node reboot time
```sh
for n in $(oc get node -oname); do echo "------ $n -------"; oc debug --quiet $n -- uptime -s; done
or
for i in $(oc get nodes -o wide | grep -v NAME | awk '{print $6}');do ssh core@$i sudo uptime; done
or
#!/bin/bash
for ip in $(oc get nodes  -o jsonpath='{.items[*].status.addresses[?(@.type=="InternalIP")].address}')
do
   echo "reboot node $ip"
   ssh -o StrictHostKeyChecking=no core@$ip sudo shutdown -r -t 3
done
```
### Network Policy (netpol)
```sh
oc get netpol -A | grep -v servicemesh | awk '{print $1}' | uniq > config
while read -r i;do oc delete netpol default-deny allow-from-openshift-ingress -n $i;done < config
```
### Registry
```sh
oc get images.config.openshift.io cluster -o jsonpath='{.status.externalRegistryHostnames}'
oc get images.config.openshift.io cluster -o jsonpath='{.status.internalRegistryHostname}'
```
### Probe
```sh
oc set probe dc/api-gateway --readiness --get-url=http://:8080/management/health/readiness --timeout-seconds=5 --initial-delay-seconds=180 --period-seconds=20 --success-threshold=1 --failure-threshold=5 --liveness --get-url=http://:8080/management/health/liveness --timeout-seconds=5 --initial-delay-seconds=180 --period-seconds=20 --success-threshold=1 --failure-threshold=5
```
### Project creation template
```
oc get template project-request -n openshift-config -o yaml
```
### Route invalid 
```sh
for i in `oc get routes -A | grep InvalidHost | awk '{print "oc -n " $1 " get route " $2}'`;do sh -c "$i";done
```
### Route best practise
```sh
oc annotate routes cdc haproxy.router.openshift.io/disable_cookies='true'
oc annotate routes cdc haproxy.router.openshift.io/balance='leastconn'
```
### Rollout dc
```sh
for i in $(oc get ns --no-headers | awk '{print $1}' | grep -v openshift);do for dc in $(oc get dc -n $i --no-headers | awk '{print $1}');do oc rollout latest dc/$dc -n $i;done;done
or
oc get project --no-headers | grep -vE "servicemesh|twistlock|stackrox|kube|conjur|openshift|default" | awk '{print $1}' > config
while read -r i;do for dci in $(oc get dc -n $i --no-headers | awk '{print $1}'); do oc rollout latest dc/$dci -n $i;    done; done < config
```
### Scale DC
```sh
oc get project --no-headers  | grep -v openshift | awk '{print $1}'  > config
while read -r line;do oc scale $(oc get dc -o name -n $line | awk '{print $1}' | grep -v "2") --replicas=0 -n $line;done < "config"
```

```sh
for cluster in $(get_clusters); do
eval ${cluster}
for i in $(oc get projects --no-headers | grep -vE "openshift|kube" | grep -E "lab|dev|qa|prod|test" | awk '{print $1}'); do oc adm policy add-role-to-group edit "my-mail groupname" -n $i;done
done
```
