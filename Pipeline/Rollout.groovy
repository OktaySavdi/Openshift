/////////////////////////////////////////////////////////////////
//  Name    : Oktay Savdi,Burhan Uguz
//  Require : Pipeline Utilty Steps, Openshift Client Jenkins
//  Date    :  01.12.2020
/////////////////////////////////////////////////////////////////

def apiRequest(String apiReq, String method) {
    withCredentials([string(credentialsId: "oktayouth", variable: 'TOKEN')]) {
        sh ( 
            script: "curl -k -X ${method} -H \"Authorization: Bearer ${TOKEN}\" ${apiReq}",
            returnStdout: true 
        )
    }
}

def dcroller(String service) {
                openshift.selector("dc", service).rollout().latest()
                def latestDeploymentVersion = openshift.selector('dc', service).object().status.latestVersion
                def rc = openshift.selector('rc', "${service}-${latestDeploymentVersion}")
                timeout (time: 10, unit: 'MINUTES') {
                    rc.untilEach(1){
                        def rcMap = it.object()
                        return (rcMap.status.replicas.equals(rcMap.status.readyReplicas))
                    }
                }
}

pipeline {
    //Servers
    agent any

    //Parameters
    parameters {
        booleanParam ( name: 'All_Namespace', defaultValue: false,   description: 'Tüm Projeleri için Rollout' )
        booleanParam ( name: 'SERVICE', defaultValue: false, description: 'Servis Bazlı Rollout')
        text         (name: 'VARIABLES', defaultValue: '', description: 'Order Bazlı Rollout') 
    }

    stages{        
            stage('action'){
                steps{
                        script
                             {
                                openshift.withCluster( 'sandbox') { 
                                    // OCP rest api ile proje isimleri alınır
                                    namespaces = readJSON text: apiRequest("\"https://myapiserver:6443/apis/project.openshift.io/v1/projects\"", "GET")
                                    
                                    if ( params.SERVICE == true ) {
                                            //Boolen params ve text kontrolleri yapılır
                                            if (params.All_Namespace == false)
                                                { 
                                                 if (params.VARIABLES.isEmpty())
                                                    {  error('Namespace adı girilmeli!!') }
                                                }else{error('Servis veya All Namespace sadece birini seçebilirsiniz') }

                                            //test alanı parse edilerek array içine alınır
                                            def ns = params.VARIABLES.split('\n').collect{"${it}"}
                                            //Array içindeki her obje için
                                            ns.each{
                                                ns = "${it}"
                                                //Proje var mı kontrolü yapılır
                                                if (namespaces.items.metadata.name.find { it == ns }){
                                                    //Her proje içine gidilir
                                                    openshift.withProject("${it}"){
                                                        //DeploymentConfig listesi çekilir
                                                        def dcSelector = openshift.selector( 'deploymentconfig' )
                                                        //DC varlık kontrolü yapılır
                                                        if (dcSelector.exists()){
                                                          //her dc için rollout çekilir
                                                          dcSelector.withEach 
                                                          {                                                             
                                                             //yukarıda tanımlanan fonksiyon çekilir
                                                            dcroller("${it.name().replaceAll('deploymentconfig/','')}")
                                                           }
                                                         }else{
                                                            echo "######### ${ns} Projesinde DC Bulunamadi ###################"
                                                         }
                                                    }
                                                }else{
                                                    echo "######### Proje Bulunamadi - ${ns} ###################"
                                                }
                                            }                                          
                                    } 
                                    // Tüm namespace seçildi ise
                                    else if( params.All_Namespace == true) {                     
                                    
                                    //For each ile her proje içine girilir
                                    namespaces.items.metadata.name.each
                                    {
                                        // Openshift projesi olmayanlar filtrelenir
                                        if ( !("${it}" =~ /^openshift(.*)/) ) 
                                        { 
                                            //Her proje içine gidilir
                                            openshift.withProject("${it}"){
                                                //DeploymentConfig listesi çekilir
                                                def dcSelector = openshift.selector( 'deploymentconfig' )
                                                //her dc için rollout çekilir
                                                dcSelector.withEach { 
                                                //yukarıda tanımlanan fonksiyon çekilir
                                                dcroller("${it.name().replaceAll('deploymentconfig/','')}")
                                                }
                                            }
                                        }
                                    }

                                    }  else {
                                            if (params.VARIABLES.isEmpty()){  error('Namespace ve Servis adı girilmeli!!') }
                                             //test alanı parse edilerek array içine alınır
                                            def orders = params.VARIABLES.split('\n').collect{"${it}"}
                                            //Array içindeki her obje için
                                            orders.each{
                                                order = it.split(/[.]/).collect{"${it}"}
                                                //Proje var mı kontrolü yapılır
                                                if (namespaces.items.metadata.name.find { it == "${order[0]}" }){
                                                    //Her proje içine gidilir
                                                    openshift.withProject("${order[0]}"){
                                                        //DC varlık kontrolü yapılır
                                                        if (openshift.selector('deploymentconfig',"${order[1]}").exists()){
                                                        //yukarıda tanımlanan fonksiyon çekilir
                                                        dcroller("${order[1]}")
                                                        }else{
                                                            echo "######### ${order[0]} Projesinde ${order[1]} Bulunamadi ###################"
                                                        }
                                                    }
                                                }else{
                                                    echo "######### Proje Bulunamadi - ${order[0]} ###################"
                                                }
                                            }                                          
                                    }                             

                                }                    
                                
                            }
                    }
            }
    }
}
