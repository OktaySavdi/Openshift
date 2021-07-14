
### Openshift Install Logging

**Check the existence of the elasticsearch operator namespace**
```bash
oc get namespace --field-selector=metadata.name="openshift-operators-redhat"  -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the elasticsearch operator namespace, if it was not found**
```bash
oc get namespace --field-selector=metadata.name="openshift-operators-redhat"  -o=jsonpath="{.items[*]['metadata.name']}"
```
**Create the elasticsearch operator namespace, if it was not found**
```bash
cat <<EOF | oc create -f -
apiVersion: v1
kind: Namespace
metadata:
  name: openshift-operators-redhat 
  annotations:
    openshift.io/node-selector: ""
  labels:
    openshift.io/cluster-monitoring: "true"
EOF
```
**Check the creation of the elasticsearch operator namespace, if it was not found**
```bash
oc get namespace --field-selector=metadata.name="openshift-operators-redhat"  -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-operators-redhat
```
**Check the creation of the elasticsearch operator namespace, if it was not found**
```bash
 oc get namespace --field-selector=metadata.name="openshift-logging"  -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-operators-redhat
```
**Check the existence of the elasticsearch operator subscription**
```bash
oc get subscription --field-selector=metadata.name="elasticsearch-operator" -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-operators-redhat
```
**Create the elasticsearch operator subscription, if it was not found**
```bash
cat <<EOF | oc create -f -
apiVersion: v1
kind: Namespace
metadata:
  name: openshift-operators-redhat 
  annotations:
    openshift.io/node-selector: ""
  labels:
    openshift.io/cluster-monitoring: "true"
EOF
```
**Check the creation of the elasticsearch operator subscription, if it was not found**
```bash
oc get subscription --field-selector=metadata.name="elasticsearch-operator" -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-operators-redhat
```
**Check the readiness of the elasticsearch cluster service version**
```bash
oc get clusterserviceversions.operators.coreos.com --all-namespaces | grep -i succeeded | grep 'elasticsearch-operator' | wc -l
```
**Check the existence of the cluster logging operator group**
```bash
oc get operatorgroup --field-selector=metadata.name="cluster-logging"  -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-logging
```
**Create the cluster logging operator group, if it was not found**
```bash
cat <<EOF | oc create -f -
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: cluster-logging
  namespace: openshift-logging 
spec:
  targetNamespaces:
  - openshift-logging
EOF
```
**Check the creation of the cluster logging operator group, if it was not found**
```bash
oc get operatorgroup --field-selector=metadata.name="openshift-operators-redhat"  -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-operators-redhat
```
**Check the existence of the cluster logging operator subscription**
```bash
oc get subscription --field-selector=metadata.name="cluster-logging" -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-logging
```
**Create the cluster logging operator subscription, if it was not found**
```bash
cat <<EOF | oc create -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cluster-logging
  namespace: openshift-logging 
spec:
  channel: "4.7" 
  name: cluster-logging
  source: redhat-operators 
  sourceNamespace: openshift-marketplace
EOF
```
**Check the creation of the cluster logging operator subscription, if it was not found**
```bash
oc get subscription --field-selector=metadata.name="cluster-logging" -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-logging
```
**Check the readiness of the cluster logging cluster service version**
```bash
oc get clusterserviceversions.operators.coreos.com --all-namespaces | grep -i succeeded | grep 'clusterlogging'
```
**Check the existence cluster logging instance**
```bash
oc get clusterloggings.logging.openshift.io --field-selector=metadata.name=instance -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-logging
```
**Create the cluster logging operator subscription, if it was not found**
```bash
cat <<EOF | oc create -f -
apiVersion: "logging.openshift.io/v1"
kind: "ClusterLogging"
metadata:
  name: "instance"
  namespace: "openshift-logging"
spec:
  managementState: "Managed"  
  logStore:
    type: "elasticsearch"  
    retentionPolicy: 
      application:
        maxAge: 1d
      infra:
        maxAge: 7d
      audit:
        maxAge: 7d
    elasticsearch:
      resources:
        limits:
          memory: 10Gi
        requests:
          cpu: 500m
          memory: 10Gi
      nodeCount: 3
      storage: {}
      redundancyPolicy: "SingleRedundancy"
  visualization:
    type: "kibana"  
    kibana:
      replicas: 1
  curation:
    type: "curator"
    curator:
      schedule: "30 3 * * *" 
  collection:
    logs:
      type: "fluentd"  
      fluentd: {}
EOF
```
**Check the creation of the cluster logging instance, if it was not found**
```bash
oc get clusterloggings.logging.openshift.io --field-selector=metadata.name=instance -o=jsonpath="{.items[*]['metadata.name']}" -n openshift-logging
```
