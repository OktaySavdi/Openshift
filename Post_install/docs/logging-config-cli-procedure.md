# LOGGING CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- HTTP endpoint for log forwarding (Splunk, ELK, Loki, etc.)
- OpenShift Logging v6.x or later

## OVERVIEW
Configure OpenShift Logging v6.x using `ClusterLogForwarder` to collect and forward logs from application pods, infrastructure components, and audit sources to an HTTP endpoint. No `ClusterLogging` instance is needed in v6.x.

## STEP 1: CREATE LOGGING NAMESPACE
```yaml
cat <<EOF | oc apply -f -
apiVersion: v1
kind: Namespace
metadata:
  name: openshift-logging
  annotations:
    openshift.io/node-selector: ""
  labels:
    openshift.io/cluster-monitoring: "true"
EOF
```

## STEP 2: CREATE OPERATORGROUP
```yaml
cat <<EOF | oc apply -f -
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

## STEP 3: INSTALL CLUSTER LOGGING OPERATOR
```yaml
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cluster-logging
  namespace: openshift-logging
spec:
  channel: stable
  name: cluster-logging
  source: redhat-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
EOF

# Wait for operator
oc get csv -n openshift-logging -w
```

## STEP 4: SERVICEACCOUNT FOR COLLECTOR
```yaml
cat <<EOF | oc apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: collector
  namespace: openshift-logging
EOF
```

## STEP 5: RBAC FOR LOG COLLECTION
```yaml
# Application logs
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: collector-application-logs
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: collect-application-logs
subjects:
- kind: ServiceAccount
  name: collector
  namespace: openshift-logging
EOF

# Infrastructure logs
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: collector-infrastructure-logs
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: collect-infrastructure-logs
subjects:
- kind: ServiceAccount
  name: collector
  namespace: openshift-logging
EOF

# Audit logs
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: collector-audit-logs
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: collect-audit-logs
subjects:
- kind: ServiceAccount
  name: collector
  namespace: openshift-logging
EOF
```

## STEP 6: CLUSTERLOGFORWARDER WITH HTTP OUTPUT
```yaml
cat <<EOF | oc apply -f -
apiVersion: observability.openshift.io/v1
kind: ClusterLogForwarder
metadata:
  name: http-log-forwarder
  namespace: openshift-logging
spec:
  serviceAccount:
    name: collector
  managementState: Managed

  collector:
    resources:
      requests:
        cpu: 200m
        memory: 768Mi
      limits:
        cpu: 2
        memory: 1Gi

  inputs:
  - name: applications
    type: application
    application: {}
  - name: infrastructure
    type: infrastructure
    infrastructure: {}
  - name: audits
    type: audit
    audit:
      sources: [kubeAPI, openshiftAPI, auditd]

  filters:
  - name: enablemultiline
    type: detectMultilineException
  - name: drop-system-namespaces
    type: drop
    drop:
    - test:
      - field: .kubernetes.namespace_name
        matches: "^kube-.*$"
    - test:
      - field: .kubernetes.namespace_name
        matches: "^default$"
    - test:
      - field: .kubernetes.namespace_name
        matches: "^openshift-.*$"
      - field: .kubernetes.namespace_name
        notMatches: "^openshift-ingress$"
  - name: drop-probes-regex
    type: drop
    drop:
    - test:
      - field: .message
        matches: '(?i)(get\s+(/healthz|/ready|/metrics))|probe\s+succeeded|health\s+check'
  - name: drop-lowlevel
    type: drop
    drop:
    - test:
      - field: .level
        matches: "^(trace|debug)$"
    - test:
      - field: .message
        matches: "^(\\s*|-|null|NULL|~)$"
  - name: drop-ocp-audit
    type: kubeAPIAudit
    kubeAPIAudit:
      omitStages: [RequestReceived, ResponseStarted]
      rules:
      - level: None
        verbs: [get, list, watch]
      - level: None
        users: ["system:*"]
        verbs: [get, list, watch]
      - level: Metadata
        verbs: [delete, create, update, patch]
      - level: RequestResponse
        user: ["*"]

  outputs:
  - name: http-output
    type: http
    http:
      url: "http://your-log-server:8080/logs"
      method: POST
      timeout: 60
      headers:
        Content-Type: "application/json"
        X-Log-Source: "openshift-cluster"
      batch:
        maxBytes: 102400
        maxRecords: 1000
        timeoutSecs: 5

  pipelines:
  - name: app-pipeline
    inputRefs: [applications]
    filterRefs: [enablemultiline, drop-system-namespaces, drop-lowlevel, drop-probes-regex]
    outputRefs: [http-output]
  - name: infra-pipeline
    inputRefs: [infrastructure]
    filterRefs: [drop-lowlevel, drop-probes-regex]
    outputRefs: [http-output]
  - name: audit-pipeline
    inputRefs: [audits]
    filterRefs: [drop-ocp-audit]
    outputRefs: [http-output]
EOF
```

## VERIFICATION
```bash
oc get csv -n openshift-logging
oc get deployment -n openshift-logging cluster-logging-operator
oc get clusterlogforwarder -n openshift-logging
oc get pods -n openshift-logging -l app.kubernetes.io/component=collector
oc get daemonset -n openshift-logging
oc logs -n openshift-logging -l app.kubernetes.io/component=collector --tail=50
oc get clusterrolebinding | grep collector
oc run test-log --image=busybox --rm -it --restart=Never -- sh -c "echo 'Test log message'; sleep 5"
```

## TROUBLESHOOTING
```bash
oc get pods -n openshift-logging
oc describe pod -n openshift-logging -l app.kubernetes.io/component=collector
oc describe clusterlogforwarder http-log-forwarder -n openshift-logging
oc describe daemonset -n openshift-logging
oc logs -n openshift-logging -l app.kubernetes.io/component=collector --tail=100
oc logs -n openshift-logging deployment/cluster-logging-operator
oc get clusterrolebinding collector-application-logs -o yaml
oc get clusterrolebinding collector-infrastructure-logs -o yaml
oc get clusterrolebinding collector-audit-logs -o yaml
oc debug node/worker-0 -- chroot /host curl -X POST http://your-log-server:8080/logs -H "Content-Type: application/json" -d '{"test":"message"}'
oc get clusterlogforwarder http-log-forwarder -n openshift-logging -o jsonpath='{.status.conditions}' | jq
```

## DELETE LOGGING
```bash
oc delete clusterlogforwarder http-log-forwarder -n openshift-logging
oc delete clusterrolebinding collector-application-logs collector-infrastructure-logs collector-audit-logs
oc delete serviceaccount collector -n openshift-logging
oc delete subscription cluster-logging -n openshift-logging
oc delete namespace openshift-logging
```

## CONFIGURATION OPTIONS (EXAMPLES)
```yaml
# Syslog output
- name: syslog-output
  type: syslog
  syslog:
    facility: user
    severity: informational
  url: 'tls://syslog.example.com:514'

# Kafka output
- name: kafka-output
  type: kafka
  kafka:
    brokers: [kafka-broker1:9092, kafka-broker2:9092]
    topic: openshift-logs

# Loki output
- name: loki-output
  type: loki
  loki:
    url: https://loki.example.com:3100

# Splunk output
- name: splunk-output
  type: splunk
  splunk:
    url: https://splunk.example.com:8088
    token: <hec-token>

# CloudWatch output
- name: cloudwatch-output
  type: cloudwatch
  cloudwatch:
    region: us-east-1
    logGroupName: /openshift/logs
```

## FILTER PATTERNS SUMMARY
- Multiline exception detection (Java/Python/Node stack traces)
- System namespace filtering (drops kube-* default openshift-* except ingress)
- Health check filtering (drops /healthz, /ready, /metrics, probe success)
- Log level filtering (drops trace/debug & empty messages)
- Audit filtering (reduces noisy verbs; metadata only for CRUD; full for others)

## BEST PRACTICES
- Use HTTP output for flexibility
- Apply filters to reduce volume (50–70% typical)
- Drop health checks & debug logs in production
- Keep `openshift-ingress` for HAProxy access logs
- Filter audit logs aggressively
- Set resource limits for collector
- Test filters before production
- Batch settings improve network efficiency
- Include cluster identification headers

## HTTP ENDPOINT REQUIREMENTS
Your log server must:
- Accept HTTP POST & `application/json`
- Handle batched entries
- Return 2xx for success
- Support ≥60s timeout

Example received log:
```json
{
  "message": "Application log message",
  "kubernetes": {
    "namespace_name": "myapp",
    "pod_name": "myapp-12345-abcde",
    "container_name": "app"
  },
  "level": "info",
  "@timestamp": "2024-11-14T10:30:00Z"
}
```

## LOG TYPES
- application: User pod logs (includes ingress HAProxy)
- infrastructure: Node/system components (kubelet, crio, systemd)
- audit: Kubernetes/OpenShift API + Linux auditd

## NOTES
- v6.x uses `ClusterLogForwarder` directly (no `ClusterLogging`)
- Vector collector DaemonSet on all nodes
- ServiceAccount + three ClusterRoleBindings required
- Filters reduce volume & cost
- HTTP batching saves bandwidth
- `openshift-ingress` explicitly allowed
