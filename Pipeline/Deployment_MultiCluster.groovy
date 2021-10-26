pipeline {
    //Servers
    agent any

    //Parameters
    parameters {
        string(name: 'PROJECT_NAME', defaultValue: '', description: 'Project Name')
        string(name: 'APP_NAME', defaultValue: '', description: 'Application Name')
        string(name: 'IMAGE_NAME', defaultValue: '', description: 'Image Name ( nginx:1.19.0 ) ')
    }

    stages{        
        stage('control')
        {
            steps
            {
                script
                {
                    if (!params.PROJECT_NAME.isEmpty())
                    { 
                        if (!params.APP_NAME.isEmpty())
                        { 
                            if (params.IMAGE_NAME.isEmpty())
                            {  error('Image Empty') }
                        }else{error('Application Empty')}
                    }else{error('Project Empty')}                              
                }
            }    
        }
            stage('action'){
                steps{
                        script
                             {
                               def clusters = [
                              [
                                apiUrl: "https://api.hb1.oc.local:6443",
                                registryUrl: "docker://default-route-openshift-image-registry.apps.hb1.oc.local",
                                nonprodApiUrl: "https://api.hosting1.oc.local:6443",
                                nonprodRegistryUrl: "docker://default-route-openshift-image-registry.apps.hosting1.oc.local",
                              ],
                              [
                                apiUrl: "https://api.hb2.oc.local:6443",
                                registryUrl: "docker://default-route-openshift-image-registry.apps.hb2.oc.local",
                                nonprodApiUrl: "https://api.hosting2.oc.local:6443",
                                nonprodRegistryUrl: "docker://default-route-openshift-image-registry.apps.hosting2.oc.local",
                              ]
                            ]
                            for (int i=0; i < clusters.size(); ++i) {
                              def cluster = clusters[i]

								withEnv(["OCP_PROD_API_URL=${cluster.apiUrl}", "OCP_PROD_REGISTRY_URL=${cluster.registryUrl}", "OCP_NONPROD_API_URL=${cluster.nonprodApiUrl}", "OCP_NONPROD_REGISTRY_URL=${cluster.nonprodRegistryUrl}"]) {
									sh '''
                                      project_name=`echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]'`
                                      app_name=`echo "$APP_NAME" | tr '[:upper:]' '[:lower:]'`
                                      oc login "$OCP_NONPROD_API_URL" --username=$OCPSERVICE_USER --password=$OCPSERVICE_PASS --insecure-skip-tls-verify=true
                                      testToken=`oc whoami -t`
                                      svcRoute=$(oc get route  -n ${project_name}-qa |grep ${app_name}  |wc -l)
                                      oc login "$OCP_PROD_API_URL" --username=${OCPSERVICE_USER} --password=${OCPSERVICE_PASS} --insecure-skip-tls-verify=true
                                      prodToken=`oc whoami -t`
                                      
                                      if [ "$(oc get deployment ${app_name} -n ${project_name} > /dev/null 2>&1;echo $?)" == "0" ]; then
                                        
                                       tag=`oc get is ${app_name} -n ${project_name} -o yaml |grep tag |tail -1 | awk '{print $2}'`
                                       nextTag=$(((`echo $tag  |awk -F'.'  '{print $NF}'`)+1))  
                                       skopeo copy --src-tls-verify=false --src-creds openshift:$testToken --dest-tls-verify=false --dest-creds openshift:$prodToken ${OCP_NONPROD_REGISTRY_URL}/${project_name}-qa/${app_name}:latest ${OCP_PROD_REGISTRY_URL}/${project_name}/${app_name}:v1.$nextTag
                                       oc tag ${project_name}/${app_name}:v1.$nextTag ${project_name}/${app_name}:latest
                                       imageReference=\$(oc get  is ${app_name} -n  ${project_name} -o jsonpath="{.status.tags[?(@.tag==\\"v1.$nextTag\\")].items[*].dockerImageReference}")
                                       oc patch deployment/${app_name} -n ${project_name} -p "{\\"spec\\":{\\"template\\":{\\"spec\\":{\\"containers\\":[{\\"name\\":\\"${app_name}\\",\\"image\\": \\"${imageReference}\\" } ]}}}}" || true
                                       oc rollout status deployment/${app_name} -n ${project_name}
                                      else 
                                
                                       skopeo copy --src-tls-verify=false --src-creds openshift:$testToken --dest-tls-verify=false --dest-creds openshift:$prodToken ${OCP_NONPROD_REGISTRY_URL}/${project_name}-qa/${app_name}:latest ${OCP_PROD_REGISTRY_URL}/${project_name}/${app_name}:v1.0
                                       oc tag ${project_name}/${app_name}:v1.0 ${project_name}/${app_name}:latest
                                       oc new-app --image-stream=${app_name} ${ENV_PARAMETERS}  -e TZ=Europe/Istanbul -n ${project_name}
                                       oc set env deployment/${app_name} ${ENV_PARAMETERS} -n ${project_name}
                                       if [ $svcRoute == "1" ]; then
                                        oc expose svc $app_name -n ${project_name}
                                       fi
                                                    
                                      fi
                                    '''
								}
                            }
                    }
            }
    }
}
