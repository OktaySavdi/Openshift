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
								withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'myuser_creds', usernameVariable: 'MY_USER', passwordVariable: 'MY_PASS'],]) {
									sh '''
										project_name=`echo "${PROJECT_NAME}" | tr '[:upper:]' '[:lower:]'`
										app_name=`echo "${APP_NAME}" | tr '[:upper:]' '[:lower:]'`
                                                                                image_name=$(echo $IMAGE_NAME | cut -f1 -d:)
										APP=$(if [[ $(oc get deployment ${APP_NAME} -n $project_name | wc -l) -gt 0 ]]; then echo "deployment"; elif [[ $(oc get dc ${APP_NAME} -n $project_name  | wc -l) -gt 0 ]]; then echo "dc"; else echo "Application Not Found"; exit 1 ; fi)
										oc login https://api.hb.oc.local:6443 --username=${MY_USER} --password=${MY_PASS} --insecure-skip-tls-verify=true
										
										if [ $(oc get project "${PROJECT_NAME}" | wc -l) -gt 0 ]; then
											
											oc project $project_name
											
											if [[ $(oc get ${APP} ${APP_NAME} | wc -l) -gt 0 ]]; then

												oc patch ${APP}/${APP_NAME} --patch="{\\\"spec\\\":{\\\"template\\\":{\\\"spec\\\":{\\\"containers\\\":[{\\\"name\\\": \\\"${image_name}\\\", \\\"image\\\":\\\"${IMAGE_NAME}\\\"}]}}}}"
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
