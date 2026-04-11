# EGRESS IP CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with OVN-Kubernetes CNI
- cluster-admin access
- Static IP address(es) available for egress traffic

## OVERVIEW
Configure egress IPs to route outbound traffic from specific namespaces through dedicated IP addresses for firewall whitelisting or traffic control.

## STEP 1: LABEL WORKER NODES
```bash
# Label all worker nodes as egress-assignable
oc label nodes -l node-role.kubernetes.io/worker k8s.ovn.org/egress-assignable=""

# Or label specific nodes
oc label node worker-0 k8s.ovn.org/egress-assignable=""
oc label node worker-1 k8s.ovn.org/egress-assignable=""
```

## STEP 2: CREATE EGRESSIP RESOURCE
```yaml
# Replace with your egress IP address(es)
cat <<EOF | oc apply -f -
apiVersion: k8s.ovn.org/v1
kind: EgressIP
metadata:
  name: cluster-egress
spec:
  egressIPs:
  - 10.19.86.200
  - 10.19.86.201
  namespaceSelector:
    matchLabels:
      egress: "restricted"
  # Optional: pod-level selector
  # podSelector:
  #   matchLabels:
  #     app: "myapp"
EOF
```

## STEP 3: LABEL NAMESPACE
```bash
# Label namespace to use egress IP
oc label namespace <your-namespace> egress=restricted

# Example
oc label namespace production egress=restricted
oc label namespace finance egress=restricted
```

## VERIFICATION
```bash
# Check EgressIP status
oc get egressip

# Detailed view
oc get egressip cluster-egress -o yaml

# Check which node has the IP assigned
oc get egressip cluster-egress -o jsonpath='{.status.items[*].node}'

# Verify node labels
oc get nodes -l k8s.ovn.org/egress-assignable --show-labels

# Test from a pod in labeled namespace
oc run test-pod -n production --image=registry.access.redhat.com/ubi8/ubi -- sleep infinity
oc exec -n production test-pod -- curl -s http://ifconfig.me
# Should show your egress IP
```

## TROUBLESHOOTING
```bash
# EgressIP not assigned
oc describe egressip cluster-egress

# Check node capacity
oc get node <node-name> -o jsonpath='{.status.allocatable.k8s\.ovn\.org/egress-assignable}'

# View OVN logs
oc logs -n openshift-ovn-kubernetes -l app=ovnkube-node --tail=100

# Check namespace label
oc get namespace <namespace> --show-labels | grep egress

# Verify pod is using egress IP
oc get pod -n <namespace> -o wide
```

## CONFIGURATION OPTIONS
```yaml
# Multiple egress IPs (high availability)
egressIPs:
  - 10.19.86.200
  - 10.19.86.201
  - 10.19.86.202

# Pod-level selector (specific apps only)
podSelector:
  matchLabels:
    app: "database"
    tier: "backend"

# Multiple namespace selector
namespaceSelector:
  matchExpressions:
  - key: environment
    operator: In
    values: ["production", "staging"]
```

## REMOVE EGRESS IP
```bash
# Remove from namespace
oc label namespace <namespace> egress-

# Delete EgressIP resource
oc delete egressip cluster-egress

# Remove node labels (optional)
oc label nodes -l k8s.ovn.org/egress-assignable k8s.ovn.org/egress-assignable-
```

## NOTES
- Egress IPs must be in the same subnet as worker nodes
- IPs are automatically distributed across labeled nodes
- High availability: If a node fails, IP moves to another node
- Maximum egress IPs per node: defaults to cluster limits
- Traffic from unlabeled namespaces uses default routing
- Works with OVN-Kubernetes CNI only (not OpenShift SDN)

## ADVANCED CONFIGURATION
```bash
# Set max egress IPs per node
oc patch network.operator cluster --type=merge \
  -p '{"spec":{"defaultNetwork":{"ovnKubernetesConfig":{"gatewayConfig":{"routingViaHost":false,"ipForwarding":"Restricted"}}}}}'

# Multiple EgressIP resources
cat <<EOF | oc apply -f -
apiVersion: k8s.ovn.org/v1
kind: EgressIP
metadata:
  name: finance-egress
spec:
  egressIPs:
  - 10.19.86.210
  namespaceSelector:
    matchLabels:
      department: "finance"
---
apiVersion: k8s.ovn.org/v1
kind: EgressIP
metadata:
  name: hr-egress
spec:
  egressIPs:
  - 10.19.86.211
  namespaceSelector:
    matchLabels:
      department: "hr"
EOF
```

## USE CASES
1. Firewall whitelisting for specific applications
2. Compliance requirements for traffic isolation
3. Third-party API access with static IPs
4. Database connections requiring source IP whitelisting
5. Audit trail for outbound connections
