pipeline {
    //Servers
    agent any

    //Parameters
    parameters {
        string(name: 'PROJECT_NAME', defaultValue: '', description: 'Project Name')
        string(name: 'APP_NAME', defaultValue: '', description: 'Application Name')
        string(name: 'Mount_Address', defaultValue: '', description: 'Mount Path ( /data )')
        string(name: 'FILE_NAME', defaultValue: '', description: 'File Name ( application.properties )')
        string(name: 'CONFIG_NAME', defaultValue: '', description: 'ConfigMap Name ( myconfig )') 
        text(name: 'VARIABLES', defaultValue: '', description: 'Enter Variables') 
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
                            if (!params.FILE_NAME.isEmpty())
                            { 
                                if (!params.VARIABLES.isEmpty())
                                { 
                                    if (params.CONFIG_NAME.isEmpty())
                                    { 
                                        error('ConfimapName Empty')
                                    }
                                }else{error('Variable Empty')}
                            }else{error('FileName Empty')}
                        }else{error('Application Empty')}
                    }else{error('Project Empty')}                              
                }
            }    
        }
            stage('action'){
                steps{
                        script
                             {
								withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'My_creds', usernameVariable: 'MY_USER', passwordVariable: 'MY_PASS'],]) {
									sh '''
										project_name=`echo "${PROJECT_NAME}" | tr '[:upper:]' '[:lower:]'`
										app_name=`echo "${APP_NAME}" | tr '[:upper:]' '[:lower:]'`
										APP=$(if [[ $(oc get deployment ${APP_NAME} -n $project_name | wc -l) -gt 0 ]]; then echo "deployment"; elif [[ $(oc get dc ${APP_NAME} -n $project_name  | wc -l) -gt 0 ]]; then echo "dc"; else echo "Application Not Found"; exit 1 ; fi)
										oc login https://api.hb.oc.local:6443 --username=${MY_USER} --password=${MY_PASS} --insecure-skip-tls-verify=true
										
										if [ $(oc get project "${PROJECT_NAME}" | wc -l) -gt 0 ]; then
											
											oc project $project_name
											
											if [[ $(oc get ${APP} ${APP_NAME} | wc -l) -gt 0 ]]; then
												
												echo "${VARIABLES}" > ${FILE_NAME}
												if [[ ! $(oc get configmap ${CONFIG_NAME}) ]]; then
													oc create configmap ${CONFIG_NAME} --from-file=${FILE_NAME}
												else
													oc delete configmap ${CONFIG_NAME}
													oc create configmap ${CONFIG_NAME} --from-file=${FILE_NAME}
												fi
												oc set volumes ${APP}/${APP_NAME}  --add --overwrite=true --name=${CONFIG_NAME} --mount-path=${Mount_Address} -t configmap --configmap-name=${CONFIG_NAME}
												oc rollout status ${APP}/${APP_NAME} -n ${PROJECT_NAME} --watch
												
											else
												echo "Deployment $APP_NAME Not Found"
												exit 3
											fi
										else
											echo "Proje $PROJECT_NAME Not Found"
											exit 2
										fi
									'''
								}
                            }
                    }
            }
    }
}
