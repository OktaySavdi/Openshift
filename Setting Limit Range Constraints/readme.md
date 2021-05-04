## Setting Limit Range Constraints

If the resource violates any of the enumerated constraints, then the resource is rejected.

If the resource does not set an explicit value, and if the constraint supports a default value, then the default value is applied to the resource.

```yaml
apiVersion: "v1"
kind: "LimitRange"
metadata:
  name: "limits" 
spec:
  limits:
  - default:
      cpu: "1"
      memory: 1024Mi
    defaultRequest:
      cpu: 100m
      memory: 256Mi
    type: Container
```
```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: limit-profiles
spec:
  limits:
  - max:
      cpu: "800m"
      memory: "1Gi"
    min:
      cpu: "100m"
      memory: "99Mi"
    default:
      cpu: "700m"
      memory: "900Mi"
    defaultRequest:
      cpu: "110m"
      memory: "111Mi"
    type: Container
```
```ruby
oc create -f limit.yaml
kubectl get limitranges
oc describe limitranges limits

oc run map --image=quay.io/oktaysavdi/map --restart=Always --port=8080
oc get pod <Pod Name> -o yaml | grep cpu
```
