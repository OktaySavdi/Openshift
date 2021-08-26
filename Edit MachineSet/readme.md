### View Machinsets
```
oc get machinesets -n openshift-machine-api
```
### Manually scale machineset option 1
```
oc scale --replicas=2 machinesets <machineset> -n openshift-machine-api
```
### Manually scale machineset option 2
```
oc edit machinesets <machineset> -n openshift-machine-api
```
