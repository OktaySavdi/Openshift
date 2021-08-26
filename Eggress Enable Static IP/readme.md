## Enable Static Eggress IP in Openshift

By configuring an egress IP address for a project, all outgoing external connections from the specified project will share the same, fixed source IP address.
An egress IP address assigned to a project is different from the egress router, which is used to send traffic to specific destinations.

Egress IP addresses are implemented as additional IP addresses on the primary network interface of the node and must be in the same subnet as the node’s primary IP address.

Manually assigned, Multiple egress IP addresses per namespace are supported.

**Configuring automatically assigned egress IP addresses for a namespace**

1- A project named UPY is created.
```ruby
oc new-project upy
```
2- By obtaining the Node IP information, the IP is taken from the same subnet for the namespace.
```ruby
oc get nodes -o wide
```
3- For example, to set node1 host egress IP addresses in the range 192.168.1.0 to 192.168.1.255 (optional)
```ruby
oc patch hostsubnet node1 --type=merge -p '{"egressCIDRs": ["192.168.1.0/24"]}'
```
```ruby
oc get hostsubnet
```
4- Run the following command to assign 192.168.1.100 IP to the UPY namespace
```ruby
oc patch netnamespace upy --type=merge -p '{"egressIPs": ["192.168.1.100"]}'
```
5- The set IP is checked
```ruby
oc get netnamespace upy
```
**Configuring manually assigned egress IP addresses for a namespace**

1- A project named UPY is created.
```ruby
oc new-project upy
```
2- By obtaining the Node IP information, the IP is taken from the same subnet for the namespace.
```ruby
oc get nodes -o wide
```
3- For example, to specify that node1 should have the egress IPs 192.168.1.100, 192.168.1.101, and 192.168.1.102:
```ruby
oc patch hostsubnet node1 --type=merge -p '{"egressIPs": ["192.168.1.100", "192.168.1.101", "192.168.1.102"]}'
```
```ruby
oc get hostsubnet
```
4- For example, to assign the upy project to an IP address of 192.168.1.100:
```ruby
oc patch netnamespace upy --type=merge -p '{"egressIPs": ["192.168.1.100"]}'
```
5- The set IP is checked
```ruby
oc get netnamespace upy
```
6- Delete Egres IP which set on namespace
```ruby
oc patch netnamespace oktay-prod --type json -p '[{ "op": "remove", "path": "/egressIPs" }]'
```
