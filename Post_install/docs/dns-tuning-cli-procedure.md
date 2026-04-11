# DNS Tuning Configuration - OpenShift CLI Procedure

## Prerequisites
- Cluster-admin access
- DNS performance goals defined
- Tuned operator installed

## Overview
Optimize CoreDNS performance (caching, latency) via DNS operator settings and Tuned profile.

## Step 1: Inspect Current DNS
```bash
oc get dns.operator.openshift.io/default -o yaml
oc get pods -n openshift-dns
oc get configmap/dns-default -n openshift-dns -o yaml
oc run dns-test --image=registry.access.redhat.com/ubi8/ubi --rm -it -- bash
nslookup kubernetes.default.svc.cluster.local
```

## Step 2: Configure Cache TTLs
```bash
oc patch dns.operator.openshift.io/default --type merge -p '{"spec":{"cache":{"successTTL":"30s","denialTTL":"10s"}}}'
```

## Step 3: Create Tuned Profile
```yaml
apiVersion: tuned.openshift.io/v1
kind: Tuned
metadata:
  name: dns-tuning
  namespace: openshift-cluster-node-tuning-operator
spec:
  profile:
  - name: openshift-dns-performance
    data: |
      [main]
      summary=Optimize DNS performance for OpenShift
      include=openshift-node

      [sysctl]
      net.ipv4.ip_local_port_range=1024 65535
      net.ipv4.tcp_tw_reuse=1
      net.core.somaxconn=32768
      net.ipv4.tcp_max_syn_backlog=8192
      net.core.netdev_max_backlog=5000
      net.ipv4.ip_local_reserved_ports=5353

      [vm]
      vm.swappiness=10
  recommend:
  - match:
    - label: node-role.kubernetes.io/worker
    priority: 20
    profile: openshift-dns-performance
```
```bash
oc apply -f dns-tuned.yaml
```

## Step 4: Scale CoreDNS (if supported)
```bash
oc get deployment dns-default -n openshift-dns
# Example patch (implementation may vary by version)
oc patch dns.operator.openshift.io/default --type merge -p '{"spec":{"nodePlacement":{"replicas":3}}}'
```

## Step 5: Forwarding Configuration
```yaml
spec:
  servers:
  - name: corporate-dns
    zones:
    - example.com
    forwardPlugin:
      upstreams:
      - 10.0.0.10
      - 10.0.0.11
  - name: external-dns
    zones:
    - "."
    forwardPlugin:
      policy: Random
      upstreams:
      - 8.8.8.8
      - 8.8.4.4
```
```bash
oc edit dns.operator.openshift.io/default
```

## Step 6: (Advanced) Corefile Customization
Not recommended; operator manages config.
```bash
oc edit configmap/dns-default -n openshift-dns
```

## Step 7: Node Placement
```yaml
apiVersion: operator.openshift.io/v1
kind: DNS
metadata:
  name: default
spec:
  nodePlacement:
    nodeSelector:
      node-role.kubernetes.io/worker: ""
    tolerations:
    - key: node-role.kubernetes.io/infra
      effect: NoSchedule
      operator: Exists
```

## Verification
```bash
oc get dns.operator.openshift.io/default
oc get pods -n openshift-dns -o wide
oc get svc -n openshift-dns
oc run dns-perf --image=registry.access.redhat.com/ubi8/ubi --rm -it -- bash
# timing tests
nslookup kubernetes.default.svc.cluster.local
nslookup google.com
# Metrics
oc exec -n openshift-dns deployment/dns-default -- curl -s localhost:9153/metrics | grep coredns
oc logs -n openshift-dns -l dns.operator.openshift.io/daemonset-dns=default
```

## Troubleshooting
```bash
oc get pods -n openshift-dns
oc logs -n openshift-dns -l dns.operator.openshift.io/daemonset-dns=default
oc get co dns
oc describe co dns
oc run dns-debug --image=nicolaka/netshoot --rm -it -- bash
nslookup kubernetes.default
```

## Reset Customizations
```bash
oc patch dns.operator.openshift.io/default --type merge -p '{"spec":{"cache":null,"servers":null}}'
oc delete tuned dns-tuning -n openshift-cluster-node-tuning-operator
```

## Performance Tuning Guidelines
- Increase replicas for high traffic
- Use appropriate TTLs
- Forward internal zones to corporate DNS
- Monitor cache hit/miss metrics

## Monitoring Metrics
Key metrics:
- `coredns_dns_request_count_total`
- `coredns_dns_request_duration_seconds`
- `coredns_cache_hits_total`
- `coredns_cache_misses_total`

Example PromQL:
```text
rate(coredns_dns_request_count_total[5m])
```

## Best Practices
- Scale based on cluster size
- Avoid excessive logging
- Test changes in non-prod
- Document adjustments

## Notes
- DNS operator manages CoreDNS
- Default success TTL 30s, denial 10s
- Changes reload automatically
