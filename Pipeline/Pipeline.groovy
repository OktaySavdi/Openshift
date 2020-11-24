// Pipeline utilty steps
def apiRequest(String apiReq, String method) {
	sh ( script: "curl -k -X ${method} -H \"Authorization: Bearer ${oauthToken}\" ${apiReq}", returnStdout: true )
}
pipeline {
	agent any
	parameters {
		string ( name: 'namespace'     , description: 'Enter Namespace/Project' , defaultValue: '{namespace/project}'                                                                                                                    )
		string ( name: 'resourceName'  , description: 'Enter Workload Name'     , defaultValue: '{workloadName}'                                                                                                                         )
		choice ( name: 'mountType'     , description: 'Select Mount Type'       , choices: ['Secret Volume Mount','ConfigMap Volume Mount','Define Secret data as Environment Variable','Define ConfigMap data as Environment Variable'] )
	}
	stages {
		stage('Selecting Project and Resource Type') {
			steps {
				script {
					oauthUrl = readJSON text: sh( script: "curl https://api.<cluster-name>.<base-domain>:6443/.well-known/oauth-authorization-server", returnStdout: true )
					withCredentials([usernamePassword(credentialsId: "CREDID", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
						oauthToken = sh ( script: "sh -c \'curl -u \"${USERNAME}:${PASSWORD}\" --head -H \"X-CSRF-Token: 1\" \"${oauthUrl.issuer}/oauth/authorize?client_id=openshift-challenging-client&response_type=token\" | grep -o \"token=.*&\" | cut -d \"&\" -f 1 | cut -d \"=\" -f 2\'", returnStdout: true ).trim()
					}
					namespaces = readJSON text: apiRequest("\"https://api.<cluster-name>.<base-domain>:6443/apis/project.openshift.io/v1/projects\"", "GET")
					assert namespaces.items.metadata.name.find { it == "${params.namespace}" }
					namespace = "${params.namespace}"
					deploymentList       = readJSON text: apiRequest("\"https://api.<cluster-name>.<base-domain>:6443/apis/apps/v1/namespaces/${namespace}/deployments\"", "GET")
					deploymentConfigList = readJSON text: apiRequest("\"https://api.<cluster-name>.<base-domain>:6443/apis/apps.openshift.io/v1/namespaces/${namespace}/deploymentconfigs\"", "GET")
					if ( deploymentList.items.metadata.name.indexOf(params.resourceName) == -1 ) {
						if ( deploymentConfigList.items.metadata.name.indexOf(params.resourceName) == -1 ) {
							error "There is no ${params.resourceName} in neither Deployments or Deployment Configs"
						}
						else {
							resourceType = "apis/apps.openshift.io/v1/namespaces/${namespace}/deploymentconfigs"
						}
					}
					else {
						resourceType = "apis/apps/v1/namespaces/${namespace}/deployments"
					}
					containerList = readJSON text: apiRequest("\"https://api.<cluster-name>.<base-domain>:6443/${resourceType}/${params.resourceName}\"", "GET")
					if ( params.mountType == "Secret Volume Mount" || params.mountType == "Define Secret data as Environment Variable"  ) {
						mountTypeLocal = "Secret"
						dataPref       = "secretName"
						mountType      = "secret"
					}
					else {
						mountTypeLocal = "ConfigMap"
						dataPref       = "name"
						mountType      = "configMap"
					}
				}
			}
		}
		stage('Mount') {
			steps {
				script {
					if ( params.mountType == "Secret Volume Mount" || params.mountType == "ConfigMap Volume Mount" ) {
						plainMounts = input (
							message: "Enter Values of ${mountTypeLocal}",
							ok: "OK",
							parameters: [
								string ( name: "mountName" , defaultValue: "example" , description: "kind: ${mountTypeLocal}\nmetadata:\n  namespace: ${namespace}\n  name:"     ),
								string ( name: "keyName"   , defaultValue: "key"     , description: "data:"                                                                      ),
								text   ( name: "keyInfo"   , defaultValue: "value"   , description: "key's Value"                                                                ),
								string ( name: "volName"   , defaultValue: "config-volume" , description: "volumes:\n  - name:\n    ${mountType}: \n       ${dataPref}: example" ),
								string ( name: "mountPath" , defaultValue: "/tmp"          , description: "volumeMounts:\n    - name:\n      mountPath:"                         )
							]
						)
						if      ( params.mountType == "Secret Volume Mount"    || params.mountType == "Define Secret data as Environment Variable"    ) { keyInfo = sh ( script:"echo \'${plainMounts.("keyInfo")}\' | base64 | tr -d '\n'"                  , returnStdout: true ).trim() }
						else if ( params.mountType == "ConfigMap Volume Mount" || params.mountType == "Define ConfigMap data as Environment Variable" ) { keyInfo = sh ( script:"echo \'${plainMounts.("keyInfo")}\' | sed \':a;N;\$!ba;s/\\n/\\\\\\\\n/g\'" , returnStdout: true ).trim() }
						data = sh ( script: "echo {\\\"apiVersion\\\": \\\"v1\\\",\\\"data\\\": {" + "\\\"" + plainMounts.("keyName") + "\\\":" + "\\\"" + keyInfo + "\\\"" + "}, \\\"kind\\\": \\\"" + mountTypeLocal + "\\\",\\\"metadata\\\": {\\\"name\\\": \\\"" + plainMounts.("mountName") + "\\\"}}", returnStdout: true ).trim().replaceAll('\\]','').replaceAll('\\[','')
						apiRequest("-H \"Content-Type: application/json\" \"https://api.<cluster-name>.<base-domain>:6443/api/v1/namespaces/${namespace}/${mountType}s\" --data \'${data}\'", "POST")
						data = sh ( script: "echo {\\\"spec\\\":{\\\"template\\\":{\\\"spec\\\":{\\\"volumes\\\": [ {\\\"name\\\": \\\"" + plainMounts.("volName") + "\\\", \\\"" + mountType + "\\\": {\\\"" + dataPref + "\\\": \\\"" + plainMounts.("mountName") + "\\\"}} ]}}}}", returnStdout: true ).trim()
						apiRequest("-H \"Content-Type: application/strategic-merge-patch+json\" \"https://api.<cluster-name>.<base-domain>:6443/${resourceType}/${params.resourceName}\" --data \'${data}\'", "PATCH")
						for ( int i = 0; i < containerList.spec.template.spec.containers.name.size(); i++ ) {
							data = sh ( script: "echo {\\\"spec\\\":{\\\"template\\\":{\\\"spec\\\":{\\\"containers\\\": [{\\\"name\\\": \\\"" + containerList.spec.template.spec.containers[i].name + "\\\", \\\"volumeMounts\\\": [{\\\"name\\\": \\\"" + plainMounts.("volName") + "\\\", \\\"mountPath\\\": \\\"" + plainMounts.("mountPath") + "\\\"}]}]}}}}", returnStdout: true ).trim()
							apiRequest("-H \"Content-Type: application/strategic-merge-patch+json\" \"https://api.<cluster-name>.<base-domain>:6443/${resourceType}/${params.resourceName}\" --data \'${data}\'", "PATCH")
						}
					}
					else {
						plainMounts = input (
							message: "Enter Values of ${mountTypeLocal}",
							ok: "OK",
							parameters: [
								string ( name: "mountName" , defaultValue: "example" , description: "kind: ${mountTypeLocal}\nmetadata:\n  namespace: ${namespace}\n  name:" ),
								string ( name: "keyName"   , defaultValue: "key"     , description: "data:" ),
								text   ( name: "keyInfo"   , defaultValue: "value"   , description: "key's Value" )
							]
						)
						echo "${plainMounts.("keyInfo")}"
						if      ( params.mountType == "Secret Volume Mount"    || params.mountType == "Define Secret data as Environment Variable"    ) { keyInfo = sh ( script:"echo \'${plainMounts.("keyInfo")}\' | base64 | tr -d '\n'"                  , returnStdout: true ).trim() }
						else if ( params.mountType == "ConfigMap Volume Mount" || params.mountType == "Define ConfigMap data as Environment Variable" ) { keyInfo = sh ( script:"echo \'${plainMounts.("keyInfo")}\' | sed \':a;N;\$!ba;s/\\n/\\\\\\\\n/g\'" , returnStdout: true ).trim() }
						echo "${keyInfo}"
						data = sh ( script: "echo {\\\"apiVersion\\\": \\\"v1\\\",\\\"data\\\": {" + "\\\"" + plainMounts.("keyName") + "\\\":" + "\\\"" + keyInfo + "\\\"" + "}, \\\"kind\\\": \\\"" + mountTypeLocal + "\\\",\\\"metadata\\\": {\\\"name\\\": \\\"" + plainMounts.("mountName") + "\\\"}}", returnStdout: true ).trim().replaceAll('\\]','').replaceAll('\\[','')
						apiRequest("-H \"Content-Type: application/json\" \"https://api.<cluster-name>.<base-domain>:6443/api/v1/namespaces/${namespace}/${mountType}s\" --data \'${data}\'", "POST")
						for ( int i = 0; i < containerList.spec.template.spec.containers.name.size(); i++ ) {
							data = sh ( script: "echo {\\\"spec\\\":{\\\"template\\\":{\\\"spec\\\":{\\\"containers\\\":[{\\\"name\\\": \\\"" + containerList.spec.template.spec.containers[i].name + "\\\" , \\\"envFrom\\\":[{\\\"" + mountType + "Ref\\\":{ \\\"name\\\": \\\"" + plainMounts.("mountName") + "\\\"}}]}]}}}}", returnStdout: true ).trim()
							apiRequest("-H \"Content-Type: application/strategic-merge-patch+json\" \"https://api.<cluster-name>.<base-domain>:6443/${resourceType}/${params.resourceName}\" --data \'${data}\'", "PATCH")
						}
					}
				}
			}
		}
	}
	post {
		always {
			script {
				sh ( script: "curl -k -X DELETE -H \"Authorization: Bearer ${oauthToken}\" \'https://api.<cluster-name>.<base-domain>:6443/apis/oauth.openshift.io/v1/oauthaccesstokens/${oauthToken}\'" )
			}
		}
	}
}
