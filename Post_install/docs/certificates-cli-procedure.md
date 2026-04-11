# API & Ingress Certificates Configuration - OpenShift CLI Procedure

## Prerequisites
- OpenShift cluster with cluster-admin access
- `oc` CLI logged in
- cert-manager operator installed
- CA certificate and key available

## Overview
Configure custom certificates for API server and Ingress (*.apps) routes using cert-manager with custom CA.

## Step 1: Create CA Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ca-key-pair
  namespace: cert-manager
type: kubernetes.io/tls
stringData:
  tls.crt: |
    -----BEGIN CERTIFICATE-----
    <YOUR_CA_CERTIFICATE_CONTENT>
    -----END CERTIFICATE-----
  tls.key: |
    -----BEGIN PRIVATE KEY-----
    <YOUR_CA_PRIVATE_KEY_CONTENT>
    -----END PRIVATE KEY-----
```
```bash
cat <<EOF | oc apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: ca-key-pair
  namespace: cert-manager
type: kubernetes.io/tls
stringData:
  tls.crt: |
    -----BEGIN CERTIFICATE-----
    <YOUR_CA_CERTIFICATE_CONTENT>
    -----END CERTIFICATE-----
  tls.key: |
    -----BEGIN PRIVATE KEY-----
    <YOUR_CA_PRIVATE_KEY_CONTENT>
    -----END PRIVATE KEY-----
EOF
```

## Step 2: Create ClusterIssuer
```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: ca-issuer
spec:
  ca:
    secretName: ca-key-pair
```
```bash
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: ca-issuer
spec:
  ca:
    secretName: ca-key-pair
EOF
```

## Step 3: Create API Certificate
Set environment values first:
```bash
CLUSTER_NAME="your-cluster"
BASE_DOMAIN="example.com"
```
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: api-certificate
  namespace: openshift-config
spec:
  secretName: api-certificate-secret
  issuerRef:
    name: ca-issuer
    kind: ClusterIssuer
  dnsNames:
  - api.${CLUSTER_NAME}.${BASE_DOMAIN}
  duration: 8760h
  renewBefore: 720h
```
```bash
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: api-certificate
  namespace: openshift-config
spec:
  secretName: api-certificate-secret
  issuerRef:
    name: ca-issuer
    kind: ClusterIssuer
  dnsNames:
  - api.${CLUSTER_NAME}.${BASE_DOMAIN}
  duration: 8760h
  renewBefore: 720h
EOF
```

## Step 4: Create Ingress Certificate
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: ingress-certificate
  namespace: openshift-ingress
spec:
  secretName: ingress-certificate-secret
  issuerRef:
    name: ca-issuer
    kind: ClusterIssuer
  dnsNames:
  - "*.apps.${CLUSTER_NAME}.${BASE_DOMAIN}"
  duration: 8760h
  renewBefore: 720h
```
```bash
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: ingress-certificate
  namespace: openshift-ingress
spec:
  secretName: ingress-certificate-secret
  issuerRef:
    name: ca-issuer
    kind: ClusterIssuer
  dnsNames:
  - "*.apps.${CLUSTER_NAME}.${BASE_DOMAIN}"
  duration: 8760h
  renewBefore: 720h
EOF
```

## Step 5: Apply API Certificate
```bash
oc wait --for=condition=Ready certificate/api-certificate -n openshift-config --timeout=120s
oc patch apiserver cluster --type=merge -p '{"spec":{"servingCerts":{"namedCertificates":[{"names":["api.'"${CLUSTER_NAME}"'.'"${BASE_DOMAIN}"'"],"servingCertificate":{"name":"api-certificate-secret"}}]}}}'
```

## Step 6: Apply Ingress Certificate
```bash
oc wait --for=condition=Ready certificate/ingress-certificate -n openshift-ingress --timeout=120s
oc patch ingresscontroller default -n openshift-ingress-operator --type=merge -p '{"spec":{"defaultCertificate":{"name":"ingress-certificate-secret"}}}'
```

## Verification
```bash
oc get certificate -A
oc get certificate api-certificate -n openshift-config -o yaml
oc get certificate ingress-certificate -n openshift-ingress -o yaml
oc get secret api-certificate-secret -n openshift-config
oc get secret ingress-certificate-secret -n openshift-ingress
# Test API cert
echo | openssl s_client -connect api.${CLUSTER_NAME}.${BASE_DOMAIN}:6443 -servername api.${CLUSTER_NAME}.${BASE_DOMAIN} 2>/dev/null | openssl x509 -noout -issuer -subject
# Test Ingress cert
echo | openssl s_client -connect console-openshift-console.apps.${CLUSTER_NAME}.${BASE_DOMAIN}:443 -servername console-openshift-console.apps.${CLUSTER_NAME}.${BASE_DOMAIN} 2>/dev/null | openssl x509 -noout -issuer -subject
```

## Troubleshooting
```bash
oc describe certificate api-certificate -n openshift-config
oc describe certificate ingress-certificate -n openshift-ingress
oc logs -n cert-manager deployment/cert-manager -f
oc get certificaterequest -A
oc get order -A
# Manual renewal
oc delete secret api-certificate-secret -n openshift-config
oc delete certificate api-certificate -n openshift-config
```

## Certificate Renewal
- Auto-renew 30 days before expiry.
- Force renewal by deleting secret.
```bash
oc delete secret api-certificate-secret -n openshift-config
oc get certificate api-certificate -n openshift-config -o jsonpath='{.status.renewalTime}'
```

## Uninstall
```bash
oc patch apiserver cluster --type=json -p='[{"op": "remove", "path": "/spec/servingCerts"}]'
oc delete certificate api-certificate -n openshift-config
oc patch ingresscontroller default -n openshift-ingress-operator --type=json -p='[{"op": "remove", "path": "/spec/defaultCertificate"}]'
oc delete certificate ingress-certificate -n openshift-ingress
oc delete clusterissuer ca-issuer
oc delete secret ca-key-pair -n cert-manager
```

## Notes
- API server and ingress pods restart after applying
- Certificates auto-renew 30 days before expiration
- Keep CA private key secure (cert-manager namespace)
- Monitor expiration dates
