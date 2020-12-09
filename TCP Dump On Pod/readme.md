## TCP Dump On Pod

**OCP v4**

```ruby
crictl ps | grep <deploy name>
```
```ruby
cid="<Container ID>"
```
```ruby
# pid=$( crictl inspect $cid --output yaml | grep 'pid:' | awk '{ print $2 }' )
```
```ruby
# nsenter -n -t $pid -- <command> <parameters>
```
**OCP v3**
```ruby
# oc get pods -o wide -n test
```
```ruby
ruby-ex-1-2bdp8
```
```ruby
# export PROJECT_NAME=test
```
```ruby
export POD_NAME=ruby-ex-1-2bdp8
```
```ruby
# docker ps -f label=io.kubernetes.pod.name=${POD_NAME} -f 
label=io.kubernetes.pod.namespace=${PROJECT_NAME} -f 
label=io.kubernetes.docker.type=podsandbox -q
```
```ruby
19025a64e5e7
```
```ruby
# cid=19025a64e5e7
```
```ruby
nsenter -n -t $( docker inspect --format "{{ .State.Pid }}" "$cid") tcpdump -s 0 -n -i eth0 -w /tmp/$(hostname)-$(date +"%Y-%m-%d-%H-%M-%S").pcap
```
