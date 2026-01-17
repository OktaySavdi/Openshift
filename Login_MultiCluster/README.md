# Openshift Cluster Management

If you want to manage too many openshift cluster(can be appliable to kubernetes too) all of them in single shell, here is what you should.

In this repository, you will have functions and definitions on your .bash_profile. This functions will let you have your clusters context in your kubeconfig with the name of your actual cluster.

```bash

cluster_list=(
https://api.cluster1.my.ocp.cluster:6443
https://api.cluster-2.my.ocp.cluster:6443
https://cluster-3.my.ocp.cluster:8443
https://cluster4.my.ocp.cluster:8443
)

function propercontext () {
	[[ -z "${oc_user}" ]] && ocuser
	[[ -z "${oc_pass}" ]] && ocpass
	clusters=${cluster_list[@]}
	for cluster in ${clusters[@]}; do
		clusterName=$(sed "s|https://\(api\.\)\?\(.*\)|\2|" <<< ${cluster} | awk -F "." '{print $1}' | tr '-' '_')
		echo "Logging in ${clusterName} and changing the context if it does not exist"
		checkContext=$(oc config get-contexts | awk -v context=${clusterName} '{if ($1 == context) {print 1} else if ($2 == context) {print 1}}')
		[[ ${checkContext} == "1" ]] && echo "Context ${clusterName} already exits, continuing" && continue
		oc login --insecure-skip-tls-verify=true ${cluster} --username=${oc_user} --password=${oc_pass}
		[[ $? == "0" ]] && oc config rename-context "$(oc config current-context)" ${clusterName}
		[[ $? == "0" ]] && echo "alias ${clusterName}=\"oc config use-context ${clusterName}\"" >> .bash_profile
	done
}

function get_clusters () {
	echo ${cluster_list[@]} | tr ' ' '\n' | sed "s|https://\(api\.\)\?\(.*\)|\2|" | awk -F "." '{print $1}' | tr '-' '_'
}

function ocuser () {
	echo 'enter user'
	IFS= read -r  oc_user < /dev/tty
}
function ocpass () {
	echo 'enter pass'
	IFS= read -rs oc_pass < /dev/tty
}

function oclogin () {
	[[ -z "${oc_user}" ]] && ocuser
	[[ -z "${oc_pass}" ]] && ocpass
	[[ $* != "" ]] && clusters=($*) || clusters=${cluster_list[@]}
	for cluster in ${clusters[@]}; do
		cluster=$([[ -n ${1} ]] && (echo ${cluster_list[@]} | tr ' ' '\n' | grep "${cluster}") || echo ${cluster})
		clusterName=$(sed "s|https://\(api\.\)\?\(.*\)|\2|" <<< ${cluster} | awk -F "." '{print $1}' | tr '-' '_')
		echo "Logging in ${clusterName}"
		oc login --insecure-skip-tls-verify=true ${cluster} --username=${oc_user} --password=${oc_pass} 2>&1 | grep -E "Login successful.|Error"
	done
}
```

Now, you should run **propercontext** command to let all cluster's context to be renamed as their name. It will ask your openshift credentials when you do, you should have the same credentials accross all the clusters.

You should just run **oclogin** function in the script above. It will ask you to enter your user and pass for the all cluster you added in cluster_list.

But, you can also login only one or more cluster in the list with just typing that cluster's name after **oclogin**. 

e.g
```bash
oclogin cluster1 cluster_2
#It will ask you to enter username
enter username
YourUserName
#And ask you to enter password
#When you enter password, you won't see anything on the prompt. It won't be seen unless you echo ${oc_pass} variable.
enter password
YourPassword #It can't be seen when you type it.
```
The code above will let you login cluster's simultaneously. And if you want to login onto another cluster in that shell, you won't have to type it again because you already add them in your shell. Just type **oc login cluster_3** and it will let you login without asking credentials.

At last, you can switch between clusters just writing that cluster's name(it was added as an alias when you typed propercontext and when its successfull), or with kubectx too.
```bash
kubectx cluster1
cluster1 # after executing propercontext, it will add 'alias cluster1="oc config --use-context cluster1"'. That is how you can switch to that context easily.
```
