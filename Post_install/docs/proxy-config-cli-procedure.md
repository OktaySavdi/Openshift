# PROXY CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- Corporate proxy server details (URL, port, credentials)
- CA certificate if using HTTPS interception proxy

## OVERVIEW
Configure cluster-wide proxy settings for outbound internet access through corporate proxy infrastructure.

## STEP 1: CREATE PROXY CA CERTIFICATE CONFIGMAP (IF NEEDED)
```yaml
cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-ca-bundle
  namespace: openshift-config
data:
  ca-bundle.crt: |
    -----BEGIN CERTIFICATE-----
    <base64-encoded-proxy-ca-cert>
    -----END CERTIFICATE-----
EOF
```
```bash
# Or from file
oc create configmap user-ca-bundle --from-file=ca-bundle.crt=/path/to/proxy-ca.crt -n openshift-config
```

## STEP 2: CONFIGURE CLUSTER PROXY
```bash
oc get proxy.config.openshift.io/cluster -o yaml   # view
oc edit proxy.config.openshift.io/cluster          # interactive edit
```
```yaml
cat <<EOF | oc apply -f -
apiVersion: config.openshift.io/v1
kind: Proxy
metadata:
  name: cluster
spec:
  httpProxy: http://proxy.example.com:8080
  httpsProxy: http://proxy.example.com:8080
  noProxy: .cluster.local,.svc,127.0.0.1,localhost,172.30.0.0/16,10.128.0.0/14,example.com
  trustedCA:
    name: user-ca-bundle
EOF
```

## STEP 3: CONFIGURE PROXY WITH AUTHENTICATION
```bash
oc create secret generic proxy-credentials \
  --from-literal=username=proxyuser \
  --from-literal=password='ProxyP@ssw0rd' \
  -n openshift-config
```
```yaml
cat <<EOF | oc apply -f -
apiVersion: config.openshift.io/v1
kind: Proxy
metadata:
  name: cluster
spec:
  httpProxy: http://proxyuser:ProxyP@ssw0rd@proxy.example.com:8080
  httpsProxy: http://proxyuser:ProxyP@ssw0rd@proxy.example.com:8080
  noProxy: .cluster.local,.svc,127.0.0.1,localhost,172.30.0.0/16,10.128.0.0/14
  trustedCA:
    name: user-ca-bundle
EOF
```

## STEP 4: VERIFY PROXY CONFIGURATION
```bash
oc get proxy.config.openshift.io/cluster -o yaml
oc get co
oc debug node/worker-0 -- sh -c 'chroot /host curl -I https://registry.redhat.io'
oc debug node/worker-0 -- sh -c 'chroot /host curl -I https://quay.io'
```

## STEP 5: UPDATE IMAGE REGISTRY PROXY (IF NEEDED)
```yaml
cat <<EOF | oc apply -f -
apiVersion: imageregistry.operator.openshift.io/v1
kind: Config
metadata:
  name: cluster
spec:
  proxy:
    http: http://proxy.example.com:8080
    https: http://proxy.example.com:8080
    noProxy: .cluster.local,.svc,127.0.0.1,localhost
EOF
```

## VERIFICATION
```bash
oc get proxy cluster -o yaml
oc get configmap user-ca-bundle -n openshift-config -o yaml
oc get co
oc run test-proxy --image=registry.access.redhat.com/ubi8/ubi --rm -it -- bash
curl -I https://www.redhat.com
env | grep -i proxy
oc debug node/worker-0 -- chroot /host cat /etc/systemd/system/crio.service.d/10-default-env.conf
oc debug node/worker-0 -- chroot /host env | grep -i proxy
```

## TROUBLESHOOTING
```bash
oc get proxy cluster -o jsonpath='{.status}'
oc logs -n openshift-cluster-version deployment/cluster-version-operator
oc run proxy-test --image=curlimages/curl --rm -it -- sh
curl -v -x http://proxy.example.com:8080 https://registry.redhat.io
oc debug node/worker-0 -- chroot /host trust list | grep -i proxy
oc get co | grep -v 'True.*False.*False'
oc describe co <operator>
oc get network.operator.openshift.io cluster -o yaml
```

## REMOVE PROXY CONFIGURATION
```bash
oc patch proxy.config.openshift.io/cluster --type merge -p '{"spec":{"httpProxy":"","httpsProxy":"","noProxy":""}}'
```
```yaml
cat <<EOF | oc apply -f -
apiVersion: config.openshift.io/v1
kind: Proxy
metadata:
  name: cluster
spec: {}
EOF
```
```bash
oc delete configmap user-ca-bundle -n openshift-config
```

## CONFIGURATION OPTIONS
```text
Standard noProxy entries:
.cluster.local,.svc,127.0.0.1,localhost,172.30.0.0/16,10.128.0.0/14,api.example.com,api-int.example.com,.apps.example.com
CIDR: 10.0.0.0/8,172.16.0.0/12,192.168.0.0/16
Wildcards: .internal.example.com,.corp.example.com
Separate proxies: httpProxy=http://http-proxy.example.com:8080 / httpsProxy=http://https-proxy.example.com:8443
```

## INSTALL-TIME PROXY (install-config.yaml excerpt)
```yaml
proxy:
  httpProxy: http://proxy.example.com:8080
  httpsProxy: http://proxy.example.com:8080
  noProxy: .cluster.local,.example.com,.svc,127.0.0.1,localhost,172.30.0.0/16,10.128.0.0/14
additionalTrustBundle: |
  -----BEGIN CERTIFICATE-----
  <proxy-ca-cert>
  -----END CERTIFICATE-----
```

## BEST PRACTICES
- Include internal networks in noProxy
- Add service & pod CIDRs
- Include API endpoints & router domains
- Test before cluster-wide rollout
- Secure credentials (avoid plain spec if possible)
- Monitor operator health
- Document changes
- Validate after upgrades

## NOTES
- Proxy applies cluster-wide
- Internal cluster traffic not proxied
- CA bundle required for SSL intercepting proxies
- Changes trigger operator reconciliations
- May cause brief interruptions
- noProxy correctness is critical
