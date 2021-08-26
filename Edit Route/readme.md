### Disable Cookie on route
```
oc annotate routes cdc haproxy.router.openshift.io/disable_cookies='true'
```
### Change route loadbalancing algoritm
```
oc annotate routes cdc haproxy.router.openshift.io/balance='leastconn'
```
### Add a timeout to the route
```
oc annotate route <route> --overwrite haproxy.router.openshift.io/timeout=<timeout><time_unit>
Exam: oc annotate route <route> --overwrite haproxy.router.openshift.io/timeout=5s
```
### Add a desice cookie name
```
oc annotate route <route> router.openshift.io/<cookie_name>="-<cookie_annotation>"
Exam: oc annotate route <route> router.openshift.io/helloworld="-helloworld_annotation"
```
### Restrict access by whitelisting an IP
```
oc annotate route <route> haproxy.router.openshift.io/ip_whitelist="<ip1 ip2 ip3>"
```
### Enable rate limiting
```
oc annotate route <route> haproxy.router.openshift.io/rate-limit-connections=true
```
### Create Route
```
oc expose svc/frontend --hostname=www.example.com
```
```
oc create route edge --service=frontend \
    --cert=${MASTER_CONFIG_DIR}/ca.crt \
    --key=${MASTER_CONFIG_DIR}/ca.key \
    --ca-cert=${MASTER_CONFIG_DIR}/ca.crt \
    --hostname=www.example.com
```
