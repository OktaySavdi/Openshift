pipeline {
    //Servers
    agent any

    //Parameters
    parameters {
        string(name: 'PROJECT_NAME', defaultValue: '', description: 'Proje Adi')
        string(name: 'APP_NAME', defaultValue: '', description: 'Uygulama Adi (ör: cbot ) ')
        string(name: 'SECRET_NAME', defaultValue: '', description: 'Secret Adi (ör: db2auth ) ')
        string(name: 'KEY1', defaultValue: '', description: 'KEY (ör: username )') 
        string(name: 'VALUE1', defaultValue: '', description: 'VALUE (ör: admin )')
        string(name: 'KEY2', defaultValue: '', description: 'KEY (ör: password )') 
        string(name: 'VALUE2', defaultValue: '', description: 'VALUE (ör: 123 )') 
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
                            if (!params.SECRET_NAME.isEmpty())
                            { 
                                    if (!params.KEY1.isEmpty())
                                    {
                                        if (!params.VALUE1.isEmpty())
                                        {
                                            if (!params.KEY2.isEmpty())
                                             {
                                                if (params.VALUE2.isEmpty())
                                                   {
                                                    error('Value2 Alani Boş')
                                                   }
                                             }else{error('Key2 Alani Boş')}           
                                        }else{error('Value1 Alani Boş')}                    
                                    }else{error('Key1 Alani Boş')}                      
                            }else{error('Secret Alani Boş')}
                        }else{error('Uygulama Alani Boş')}
                    }else{error('Proje Alani Boş')}                              
                }
            }    
        }
            stage('action'){
                steps{
                        script
                             {
								withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'my_creds', usernameVariable: 'MY_USER', passwordVariable: 'MY_PASS'],]) {
									sh '''
										project_name=`echo "${PROJECT_NAME}" | tr '[:upper:]' '[:lower:]'`
										app_name=`echo "${APP_NAME}" | tr '[:upper:]' '[:lower:]'`
										APP=$(if [[ $(oc get deployment ${APP_NAME} -n $project_name | wc -l) -gt 0 ]]; then echo "deployment"; elif [[ $(oc get dc ${APP_NAME} -n $project_name  | wc -l) -gt 0 ]]; then echo "dc"; else echo "Uygulama Bulunamadı"; exit 1 ; fi)
										oc login https://api.hb.oc.local:6443 --username=${MY_USER} --password=${MY_PASS} --insecure-skip-tls-verify=true
										
										if [ $(oc get project "${PROJECT_NAME}" | wc -l) -gt 0 ]; then
											
											oc project $project_name
											
											if [[ $(oc get ${APP} ${APP_NAME} | wc -l) -gt 0 ]]; then
												
												if [[ ! $(oc get secret ${SECRET_NAME}) ]]; then
                                                                                                        oc create secret generic ${SECRET_NAME} --from-literal=${KEY1}=${VALUE1} --from-literal=${KEY2}=${VALUE2}
												else
													oc delete secret ${SECRET_NAME}
													oc create secret generic ${SECRET_NAME} --from-literal=${KEY1}=${VALUE1} --from-literal=${KEY2}=${VALUE2}
												fi
                                                                                                oc set env --from=secret/${SECRET_NAME} ${APP}/${APP_NAME}
												oc rollout status ${APP}/${APP_NAME} -n ${PROJECT_NAME} --watch
												
											else
												echo "Deployment $APP_NAME Bulunamadi"
												exit 3
											fi
										else
											echo "Proje $PROJECT_NAME Bulunamadi"
											exit 2
										fi
									'''
								}
                            }
                    }
            }
    }
}
