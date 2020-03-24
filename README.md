## OpenShift 4.3 Bare Metal Install Quickstart

we will go over how to get you up and running with a Red Hat OpenShift 4.3 Bare Metal install on pre-existing infrastructure. Although this quickstart focuses on the bare metal installer, this can also be seen as a “manual” way to install OpenShift 4.3.

### Required machines

The smallest OpenShift Container Platform clusters require the following hosts:

-   One temporary bootstrap machine
    
-   Three control plane, or master, machines
    
-   At least two compute machines, which are also known as worker machines

The bootstrap, control plane, and compute machines must use the Red Hat Enterprise Linux CoreOS (RHCOS) as the operating system.

**hostnames**  
```json
<_IP1_> http.hb.oc.local  
<_IP2_> lb.hb.oc.local  
<_IP3_> bootstrap.hb.oc.local  
<_IP4_> master-01.hb.oc.local  
<_IP7_> worker-01.hb.oc.local  
<_IP8_> worker-02.hb.oc.local 
```
## Installation steps

**Install Nginx /  <_IP1_> http.hb.oc.local _** 

Install Nginx server
```bash
yum install -y nginx
```
Give permission folder
```bash
chmod -R 777 /usr/share/nginx/html/
```
enable and start services
```bash
systemctl enable nginx
systemctl restart nginx
```
**Install dnsmasq /  <_IP2_> lb.hb.oc.local _** 

install dnsmasq
```bash
yum install dnsmasq -y
```  

configure /etc/hosts
```json
192.168.10.1 lb.oc.local
192.168.10.2 bootstrap.hb.oc.local
192.168.10.3 master-01.hb.oc.local
192.168.10.4 worker-01.hb.oc.local
192.168.10.5 worker-02.hb.oc.local
192.168.10.1 api.hb.oc.local api-int api-int.hb.oc.local
192.168.10.3 etcd-0.hb.oc.local
192.168.10.6 http.hb.oc.local
``` 
  
open firewall
```json
firewall-cmd --permanent --add-port=53/tcp
firewall-cmd --permanent --add-port=53/udp
firewall-cmd --reload
```
configure dnsmasq

    vi /etc/dnsmasq.d/oc.conf

```json
server=/10.168.192.in-addr.arpa/192.168.10.1
local=/hb.oc.local/
address=/apps.hb.oc.local/hb.oc.local/192.168.10.1
address=/hb.oc.local/192.168.10.1
    
srv-host=_etcd-server-ssl._tcp.hb.oc.local,etcd-0.hb.oc.local,2380,0,10
    
dhcp-range=ens192,192.168.10.1,192.168.10.6,255.255.255.0,2m
dhcp-option=ens192,3,192.168.10.254
dhcp-option=6,192.168.10.1
```
enable and start services
```bash
systemctl enable dnsmasq
systemctl restart dnsmasq
```
Configure DNS setting
```bash
nmtui
```
![image](https://user-images.githubusercontent.com/3519706/77430940-04987b00-6ded-11ea-8c45-3dab20c67a9b.png)

Restart Network Service
```bash
systemctl restart network
```

Check `cat /etc/resolv.conf`

![image](https://user-images.githubusercontent.com/3519706/77431211-707ae380-6ded-11ea-8971-3ca27cea2fa4.png)


**Install haproxy /  <_IP2_> lb.hb.oc.local _** 

Install haproxy
```bash
yum install haproxy -y
```
Configure haproxy
```bash
vi /etc/haproxy/haproxy.cfg
```
SampleHAproxyCFG
```json
#---------------------------------------------------------------------
# main frontend which proxys to the kube-api-server-6443
#---------------------------------------------------------------------
frontend kube-api-server
    bind :6443
    mode tcp
    default_backend kube-api-server-6443
#---------------------------------------------------------------------
# main frontend which proxys to the machine-config-server-22623
#---------------------------------------------------------------------
frontend machine-config-server
    bind :22623
    mode tcp
    default_backend machine-config-server-22623
#---------------------------------------------------------------------
# main frontend which proxys to the ingress-443
#---------------------------------------------------------------------
frontend https
    bind :443
    mode tcp
    default_backend ingress-443
#---------------------------------------------------------------------
# main frontend which proxys to the ingress-80
#---------------------------------------------------------------------
frontend http
    bind :80
    mode tcp
    default_backend ingress-80
#---------------------------------------------------------------------
# round robin balancing between the api-server machines
#---------------------------------------------------------------------
backend kube-api-server-6443
    balance roundrobin
    mode tcp
    server master-01.hb.oc.local 192.168.10.3:6443 check
    server bootstrap.hb.oc.local 192.168.10.2:6443 check
#---------------------------------------------------------------------
# round robin balancing between the machine-config machines
#---------------------------------------------------------------------
backend machine-config-server-22623
    balance roundrobin
    mode tcp
    server master-01.hb.oc.local 192.168.10.3:22623 check
    server bootstrap.hb.oc.local 192.168.10.2:22623 check
#---------------------------------------------------------------------
# round robin balancing between the workers
#---------------------------------------------------------------------
backend ingress-443
    balance roundrobin
    mode tcp
    server worker-01.hb.oc.local 192.168.10.4:443 check
    server worker-02.hb.oc.local 192.168.10.5:443 check
#---------------------------------------------------------------------
# round robin balancing between the workers
#---------------------------------------------------------------------
backend ingress-80
    balance roundrobin
    mode tcp
    server worker-01.hb.oc.local 192.168.10.4:80 check
    server worker-02.hb.oc.local 192.168.10.5:80 check
```
Configure Firewall
```bash
firewall-cmd --permanent --zone=public --add-port=80/tcp
firewall-cmd --permanent --zone=public --add-port=443/tcp
firewall-cmd --permanent --zone=public --add-port=6443/tcp
firewall-cmd --permanent --zone=public --add-port=22623/tcp
firewall-cmd --permanent --zone=public --add-port=8404/tcp
firewall-cmd --reload
```
Disable Selinux
```bash
setenforce 0
sestatus
vi /etc/sysconfig/selinux
SELINUX=disabled
```
Enable and restart haproxy service
```bash
systemctl enable haproxy
systemctl restart haproxy
systemctl status haproxy
```
Reboot the machine
```bash
shutdown -r now
```
Check haproxy service and stats page
```bash
systemctl status haproxy
```
**Generate SSH Private Key and add to the agent on LB /  <_IP2_> lb.hb.oc.local _**
```bash
ssh-keygen -t rsa -b 4096 -N '' -f /root/.ssh/id_rsa
eval "$(ssh-agent -s)"
ssh-add /root/.ssh/id_rsa
```
**Configure passwordless SSH from LB to HTTP server**
```bash
ssh-copy-id -i /root/.ssh/id_rsa.pub root@192.168.10.6
```

**Download the RHCOS OVA file to create template on V-Center**

Check the following openshift providers page for newer versions before download:  
[Infrastructure Provider](https://cloud.redhat.com/openshift/install "Infrastructure Provider")  
 
![image](https://user-images.githubusercontent.com/3519706/77400994-2a0d9080-6dbd-11ea-9b07-2fe551d1d79e.png)
   
* Downlaod RHCOS image
* Deploy OVF Template on V-Center  
* Downlaod pull secret
* Change the settings of VM if needed. (disk size, ram etc.)  

**Download following files to LB server  /  <_IP2_> lb.hb.oc.local _**

Check the following openshift providers page for newer versions before download:  
[Infrastructure Provider](https://cloud.redhat.com/openshift/install "Infrastructure Provider")

installer:  
https://mirror.openshift.com/pub/openshift-v4/clients/ocp/latest/openshift-install-linux-4.3.5.tar.gz

client:  
https://mirror.openshift.com/pub/openshift-v4/clients/ocp/latest/openshift-client-linux-4.3.5.tar.gz


**Extract and copy files  /  <_IP2_> lb.hb.oc.local _**

Installer:  
```bash
wget https://mirror.openshift.com/pub/openshift-v4/clients/ocp/latest/openshift-install-linux-4.3.5.tar.gz
tar -xvf openshift-install-linux-4.3.5.tar.gz
cat README.md  
mv openshift-install /usr/local/bin/  
```
  
Client:  
```bash
wget https://mirror.openshift.com/pub/openshift-v4/clients/ocp/latest/openshift-client-linux-4.3.5.tar.gz 
tar -xvf openshift-client-linux-4.3.5.tar.gz 
cat README.md  
mv oc kubectl /usr/local/bin
```
**Create install-config.yaml  /  <_IP2_> lb.hb.oc.local _**

Get pull screet from providers page  
[https://cloud.redhat.com/openshift/install](https://cloud.redhat.com/openshift/install)

Get SSH Key from LB server
```bash
cat ~/.ssh/id_rsa.pub
```
Create an installation directory to store your required installation assets in:
```bash
mkdir oc
cd oc
```
You can customize the `install-config.yaml` file to specify more details about your OpenShift Container Platform cluster’s platform or modify the values of the required parameters.

    vi install-config.yaml
```yaml
apiVersion: v1
baseDomain: oc.local 
compute:
- hyperthreading: Enabled   
  name: worker
  replicas: 0 
controlPlane:
  hyperthreading: Enabled   
  name: master 
  replicas: 1 
metadata:
  name: hb
networking:
  clusterNetwork:
  - cidr: 10.128.0.0/14 
    hostPrefix: 23 
  networkType: OpenShiftSDN
  serviceNetwork: 
  - 172.30.0.0/16
platform:
  none: {} 
fips: false 
pullSecret: '{"auths": ...}' 
sshKey: 'ssh-ed25519 AAAA...' 
```

**Creating the Kubernetes manifest and Ignition config files  /  <_IP2_> lb.hb.oc.local _**

Because you must modify some cluster definition files and manually start the cluster machines, you must generate the Kubernetes manifest and Ignition config files that the cluster needs to make its machines.
```bash
mkdir ignition
mv install-config.yaml ignition/
```
 1- Generate the Kubernetes manifests for the cluster:
```bash
openshift-install create manifests --dir=ignition/
```

 2- Modify the `manifests/cluster-scheduler-02-config.yml` Kubernetes manifest file to prevent Pods from being scheduled on the control plane machines

         a. Open the  `manifests/cluster-scheduler-02-config.yml`  file.
         
         b. Locate the  `mastersSchedulable`  parameter and set its value to  `False`.
         
         c. Save and exit the file.
     
3- Obtain the Ignition config files:
```bash
openshift-install create ignition-configs --dir=ignition/.
```
The following files are generated in the directory:

![image](https://user-images.githubusercontent.com/3519706/77402530-d94b6700-6dbf-11ea-82f5-a02b8aa89dfa.png)

4- Copy new ignition file to Nginx server
```bash
scp -r ignition/* root@192.168.10.6:/usr/share/nginx/html/
```
**Creating Red Hat Enterprise Linux CoreOS (RHCOS) machines using an ISO image  /  <_IP2_> lb.hb.oc.local _**

Before you install a cluster on bare metal infrastructure that you provision, you must create RHCOS machines for it to use. You can use an ISO image to create the machines.

Prerequisites

-   Obtain the Ignition config files for your cluster.
    
-   Have access to an HTTP server that you can access from your computer and that the machines that you create can access.

You must download the ISO file and the RAW disk file
[https://mirror.openshift.com/pub/openshift-v4/dependencies/rhcos/4.3/4.3.0/](https://mirror.openshift.com/pub/openshift-v4/dependencies/rhcos/4.3/4.3.0/)
```bash
wget https://mirror.openshift.com/pub/openshift-v4/dependencies/rhcos/4.3/4.3.0/rhcos-4.3.0-x86_64-metal.raw.gz 
scp -r rhcos-4.3.0-x86_64-metal.raw.gz  root@192.168.10.6:/usr/share/nginx/html/
```
Mount the iso file to the server and run the machine.
![https://github.com/OktaySavdi/CoreOS/blob/master/install_Fedora_CoreOS](https://user-images.githubusercontent.com/3519706/77246525-50a2be80-6c39-11ea-85cb-ed5a3d57fd9d.png)

When you see Fedora CoreOS (Live) screen press the `TAB` button and fill the section ip with your own information
```json
coreos.inst.install_dev=sda coreos.inst.image_url=http://192.168.10.6/rhcos-4.3.0-x86_64-metal.raw.gz coreos.inst.ignition_url=http://192.168.10.6/bootstrap.ign ip=dhcp
```
If you set an individual static IP address try it
```json
coreos.inst.install_dev=sda coreos.inst.image_url=http://192.168.10.6/rhcos-4.3.0-x86_64-metal.raw.gz coreos.inst.ignition_url=http://192.168.10.6/bootstrap.ign ip=192.168.10.4::10.57.165.254:255.255.255.0:bootstrap.hb.oc.local:ens192:none nameserver=192.168.10.1
```
![image](https://user-images.githubusercontent.com/3519706/77431792-4118a680-6dee-11ea-870d-edbbf648408e.png)

the installation step will start

![https://github.com/OktaySavdi/CoreOS/blob/master/install_Fedora_CoreOS](https://user-images.githubusercontent.com/3519706/77046594-53e94080-69d4-11ea-9356-63fd24341269.png)

Necessary checks are made, 

- The machine receives IP, 
- The machine can access the internet

The machine receives IP, 
`ssh core@192.168.10.2` on dns server machine

![image](https://user-images.githubusercontent.com/3519706/77432317-f4819b00-6dee-11ea-8582-593fb02b7498.png)

It is checked whether there is internet access with the command `curl www.google.com`.
![image](https://user-images.githubusercontent.com/3519706/77406134-3d245e80-6dc5-11ea-85c6-845df3afb158.png)

Check DNS,Gateway
```bash
nmcli connection
```
![image](https://user-images.githubusercontent.com/3519706/77406248-69d87600-6dc5-11ea-89d5-ffb00c535984.png)
```bash
nmcli connection show uuid fd1dbaa0-2add-3697-a91e-34e87e13e375
```
![image](https://user-images.githubusercontent.com/3519706/77432704-7d98d200-6def-11ea-8575-397fae2f64e3.png)

## Creating the cluster /  <_IP2_> lb.hb.oc.local _

To create the OpenShift Container Platform cluster, you wait for the bootstrap process to complete on the machines that you provisioned by using the Ignition config files that you generated with the installation program.

Prerequisites

-   Create the required infrastructure for the cluster.    
-   You obtained the installation program and generated the Ignition config files for your cluster.    
-   You used the Ignition config files to create RHCOS machines for your cluster.    
-   Your machines have direct internet access.    

Procedure

Monitor the bootstrap process:
```bash
ssh core@bootstrap.hb.oc.local
    
journalctl -b -f -u bootkube.service
```   
![image](https://user-images.githubusercontent.com/3519706/77407784-be7cf080-6dc7-11ea-96f1-c87aeeac34af.png)

Validate the installation
```bash
openshift-install --dir=oc/ignition/. wait-for bootstrap-complete --log-level=info
```
![image](https://user-images.githubusercontent.com/3519706/77420078-7c10df00-6dda-11ea-8e1d-d28a7a215245.png)

 **Logging in to the cluster  / <_IP2_> lb.hb.oc.local _**
```bash
export KUBECONFIG=oc/ignition/auth/kubeconfig  
oc whoami
```
![image](https://user-images.githubusercontent.com/3519706/77424211-c5186180-6de1-11ea-9893-4e0866666790.png)
    
For permanent `oc` command configuration

    vi ~/.bash_profile
```bash 
export KUBECONFIG=/root/oc/ignition/auth/kubeconfig
PATH=$PATH:$HOME/bin:$KUBECONFIG
```
 **Approving the csrs**  / <_IP2_> lb.hb.oc.local _**

 1- Confirm that the cluster recognizes the machines:
```bash
oc get nodes
```
![image](https://user-images.githubusercontent.com/3519706/77420526-40c2e000-6ddb-11ea-8a0c-d8b939720210.png)

 2- Review the pending certificate signing requests (CSRs) and ensure that the you see a client and server request with `Pending` or `Approved` status for each machine that you added to the cluster:
```bash
oc get csr
```
![image](https://user-images.githubusercontent.com/3519706/77420624-74056f00-6ddb-11ea-83f4-df91f4b4f0a2.png)

**Check the initial configuration / <_IP2_> lb.hb.oc.local _**

 1- Watch the cluster components come online:
```bash
watch -n5 oc get clusteroperators
```
![image](https://user-images.githubusercontent.com/3519706/77424265-e2e5c680-6de1-11ea-94dd-14e6523005ec.png)


**Create image registry for non-prod env. / <_IP2_> lb.hb.oc.local _**
You must configure storage for the image registry Operator. For non-production clusters, you can set the image registry to an empty directory. If you do so, all images are lost if you restart the registry.
```bash
oc patch configs.imageregistry.operator.openshift.io cluster --type merge --patch '{"spec":{"storage":{"emptyDir":{}}}}'
```
**Complete the installation / <_IP2_> lb.hb.oc.local _**

wait for clusteroperators until available
```bash
watch -n5 oc get clusteroperators
```
validate the installation
```bash
openshift-install --dir=<installation_directory> wait-for install-complete
openshift-install --dir=oc/ignition/. wait-for install-complete
```
![image](https://user-images.githubusercontent.com/3519706/77423765-f9d7e900-6de0-11ea-80f2-437a271e5499.png)
