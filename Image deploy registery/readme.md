### learn internal or external URL
```
oc get images.config cluster -o jsonpath='{.status.externalRegistryHostnames}'
oc get images.config cluster -o jsonpath='{.status.internalRegistryHostname}'
```
### load image in your docker registery
```
docker load -i redis_12.1.2.tar
```
### tag your image for new registery name
```
docker tag redis_12.1.2 default-route-openshift-image-registry.apps.mycluster.mydomain.com/redis-dev/redis:12.1.2
```
### Login Openshift registery
```
token=`oc whoami -t`
docker login -u oktaysav -e oktaysav@mydomain -p $token default-route-openshift-image-registry.apps.mycluster.mydomain.com
```
### Push local image to Openshift registery
```
docker push default-route-openshift-image-registry.apps.mycluster.mydomain.com/redis-dev/redis:12.1.2
```
### Logout Openshift registery
```
docker logout default-route-openshift-image-registry.apps.mycluster.mydomain.com
```
