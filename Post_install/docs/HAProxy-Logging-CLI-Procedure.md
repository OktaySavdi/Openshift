# HAPROXY LOGGING MANUAL CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- oc CLI logged in

## OVERVIEW
Enable HAProxy access logs for OpenShift Ingress Controller to troubleshoot routing issues, monitor traffic, and audit HTTP requests.

## STEP 1: CHECK CURRENT CONFIGURATION
```bash
# View current IngressController
oc get ingresscontroller default -n openshift-ingress-operator -o yaml

# Check if logging is already configured
oc get ingresscontroller default -n openshift-ingress-operator -o jsonpath='{.spec.logging}'
```

## STEP 2: ENABLE HAPROXY ACCESS LOGGING
```yaml
cat <<EOF | oc apply -f -
apiVersion: operator.openshift.io/v1
kind: IngressController
metadata:
  name: default
  namespace: openshift-ingress-operator
spec:
  logging:
    access:
      destination:
        type: Container
      httpLogFormat: >-
        %ci:%cp [%t] %ft %b/%s %TR/%Tw/%Tc/%Tr/%Ta %ST %B %CC %CS %tsc %ac/%fc/%bc/%sc/%rc %sq/%bq %hr %hs %{+Q}r
EOF
```

## STEP 3: WAIT FOR ROUTER PODS TO RESTART
```bash
# Watch pods restart
oc get pods -n openshift-ingress -w

# Check status
oc get pods -n openshift-ingress
```

## STEP 4: VERIFY LOGGING ENABLED
```bash
oc get ingresscontroller default -n openshift-ingress-operator -o yaml | grep -A 10 logging
oc get deployment -n openshift-ingress
```

## VIEWING LOGS
```bash
# Specific router pod
oc logs -n openshift-ingress <router-pod-name> -c router

# Follow logs
oc logs -n openshift-ingress <router-pod-name> -c router -f

# All router pods
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router

# Follow all
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router -f --tail=50

# Last 100 lines
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router --tail=100
```

## LOG FORMAT EXPLANATION
Fields:
- `%ci:%cp` Client IP:Port
- `[%t]` Timestamp
- `%ft` Frontend name
- `%b/%s` Backend/Server name
- `%TR/%Tw/%Tc/%Tr/%Ta` Response times (Total/Wait/Connect/Response/Active)
- `%ST` HTTP status
- `%B` Bytes transferred
- `%CC` Client cookie
- `%CS` Server cookie
- `%tsc` Termination state
- `%ac/%fc/%bc/%sc/%rc` Connection counts
- `%sq/%bq` Server/Backend queue
- `%hr` Request headers
- `%hs` Response headers
- `%{+Q}r` Full HTTP request (quoted)

## EXAMPLE LOG ENTRY
```
10.128.0.1:54321 [14/Nov/2025:10:30:45.123] fe_http be_myapp/pod-abc123 0/0/1/15/16 200 1234 - - ---- 1/1/0/0/0 0/0 "GET /api/status HTTP/1.1"
```
Breakdown: Client IP/Port, timestamp, frontend, backend/pod, timings, status, bytes, request line.

## FILTERING LOGS
```bash
# Errors
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router | grep " 500 "

# 404
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router | grep " 404 "

# By path
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router | grep "/api"

# By client IP
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router | grep "10.0.0.1"

# Slow requests (>1000ms)
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router | \
  awk '{split($6, t, "/"); if (t[5] > 1000) print $0}'
```

## TROUBLESHOOTING
```bash
# Logs not appearing
oc get ingresscontroller default -n openshift-ingress-operator -o yaml | grep logging

# Router pods
oc get deployment -n openshift-ingress
oc get pods -n openshift-ingress

# Operator logs
oc logs -n openshift-ingress-operator -l name=ingress-operator

# Manual restart
oc delete pod -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default
```

## DISABLE LOGGING
```yaml
cat <<EOF | oc apply -f -
apiVersion: operator.openshift.io/v1
kind: IngressController
metadata:
  name: default
  namespace: openshift-ingress-operator
spec:
  logging: null
EOF

# Or patch
oc patch ingresscontroller default -n openshift-ingress-operator --type=json \
  -p='[{"op": "remove", "path": "/spec/logging"}]'
```

## CUSTOM LOG FORMAT OPTIONS
- Minimal: `'%ci - [%t] "%r" %ST %B'`
- Detailed: `'%ci:%cp [%t] %ft %b/%s %TR/%Tw/%Tc/%Tr/%Ta %ST %B %hr %hs %{+Q}r'`
- JSON: `'{"client":"%ci","time":"%t","request":"%r","status":%ST,"bytes":%B}'`

## FORWARDING TO EXTERNAL SYSTEM
```bash
# Example rsyslog forward (env var)
oc set env deployment/router -n openshift-ingress \
  ROUTER_SYSLOG_ADDRESS=udp://syslog-server:514
```

## PERFORMANCE CONSIDERATIONS
- <5% CPU overhead
- Logs to stdout (no disk I/O)
- Use aggregation for retention
- High-traffic clusters need external storage

## USE CASES
1. Debug 404/500
2. Monitor latency
3. Identify slow backends
4. Security audits
5. Traffic analysis
6. SSL/TLS troubleshooting
7. Capacity planning

## BEST PRACTICES
1. Enable in production
2. Aggregate externally
3. Filter by namespace/route
4. Alert on errors
5. Tailor format to tooling

## VERIFICATION
```bash
curl -k https://$(oc get route -n <namespace> <route-name> -o jsonpath='{.spec.host}')
oc logs -n openshift-ingress -l ingresscontroller.operator.openshift.io/deployment-ingresscontroller=default -c router --tail=10
```

## NOTES
- Router pods restart automatically
- No persistent storage
- Use logging stack for retention
- Standard HAProxy format
