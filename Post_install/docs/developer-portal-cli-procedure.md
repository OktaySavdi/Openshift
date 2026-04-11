# DEVELOPER PORTAL CONFIGURATION - OpenShift CLI Procedure

## PREREQUISITES
- OpenShift cluster with cluster-admin access
- Web console access

## OVERVIEW
Enable OpenShift Developer Portal (Developer Catalog) for self-service application deployment using OperatorHub and templates.

## STEP 1: ENABLE DEVELOPER CATALOG
```bash
# Check current console config
oc get console.operator.openshift.io cluster -o yaml

# Enable Developer Catalog
oc patch console.operator.openshift.io cluster --type merge -p '{"spec":{"customization":{"developerCatalog":{"categories":[{"id":"languages","label":"Languages","tags":["java","javascript","nodejs","dotnet","golang","ruby","python","php"]},{"id":"databases","label":"Databases","tags":["mongodb","mysql","postgresql","mariadb","redis"]},{"id":"middleware","label":"Middleware","tags":["amq","integration","process-automation","messaging"]},{"id":"cicd","label":"CI/CD","tags":["jenkins","pipelines","tekton"]},{"id":"ai","label":"AI/ML","tags":["ai","ml","machine-learning","tensorflow"]}]}}}}'
```

## STEP 2: ENABLE CATALOG SOURCES
```bash
# Check existing CatalogSources
oc get catalogsources -n openshift-marketplace

# Enable Red Hat Operators (default)
oc get catalogsource redhat-operators -n openshift-marketplace

# Enable Certified Operators
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: certified-operators
  namespace: openshift-marketplace
spec:
  displayName: Certified Operators
  image: registry.redhat.io/redhat/certified-operator-index:v4.14
  publisher: Red Hat
  sourceType: grpc
  updateStrategy:
    registryPoll:
      interval: 10m
EOF

# Enable Community Operators
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: community-operators
  namespace: openshift-marketplace
spec:
  displayName: Community Operators
  image: registry.redhat.io/redhat/community-operator-index:v4.14
  publisher: Red Hat
  sourceType: grpc
  updateStrategy:
    registryPoll:
      interval: 10m
EOF
```

## STEP 3: CREATE SAMPLE TEMPLATES
```bash
# Add sample application templates
oc create -n openshift namespace sample-templates

cat <<EOF | oc apply -f -
apiVersion: template.openshift.io/v1
kind: Template
metadata:
  name: nodejs-example
  namespace: openshift
  annotations:
    description: "Node.js application with MongoDB"
    iconClass: "icon-nodejs"
    tags: "quickstart,nodejs,mongodb"
objects:
- apiVersion: v1
  kind: Service
  metadata:
    name: nodejs-app
  spec:
    ports:
    - port: 8080
    selector:
      app: nodejs-app
- apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: nodejs-app
  spec:
    replicas: 1
    selector:
      matchLabels:
        app: nodejs-app
    template:
      metadata:
        labels:
          app: nodejs-app
      spec:
        containers:
        - name: nodejs
          image: registry.access.redhat.com/ubi8/nodejs-16:latest
          ports:
          - containerPort: 8080
parameters:
- name: APP_NAME
  description: "Application name"
  value: "nodejs-app"
  required: true
EOF
```

## STEP 4: ENABLE SAMPLES OPERATOR
```bash
# Check Samples Operator
oc get configs.samples.operator.openshift.io cluster -o yaml

# Configure samples
oc patch configs.samples.operator.openshift.io cluster --type merge -p '{"spec":{"managementState":"Managed","architectures":["x86_64"],"samplesRegistry":"registry.redhat.io"}}'
```

## STEP 5: CUSTOMIZE QUICK STARTS
```bash
cat <<EOF | oc apply -f -
apiVersion: console.openshift.io/v1
kind: ConsoleQuickStart
metadata:
  name: sample-application-deploy
spec:
  displayName: Deploy a Sample Application
  durationMinutes: 10
  description: Learn how to deploy a sample application
  introduction: "This quick start guides you through deploying an application"
  tasks:
    - title: Create a project
      description: Create a new project
    - title: Deploy application
      description: Deploy from Developer Catalog
    - title: Access application
      description: Access the deployed application
EOF
```

## VERIFICATION
```bash
# Check Developer Catalog enabled
oc get console.operator.openshift.io cluster -o jsonpath='{.spec.customization.developerCatalog}'

# Check catalog sources
oc get catalogsources -n openshift-marketplace

# Check samples
oc get templates -n openshift

# Check quick starts
oc get consolequickstarts

# Access Developer Portal
# Navigate to: https://console-openshift-console.apps.<cluster-domain>
# Switch to Developer perspective
# Click "+Add" to see Developer Catalog
```

## TROUBLESHOOTING
```bash
# Console pods not ready
oc get pods -n openshift-console

# Check console operator logs
oc logs -n openshift-console-operator deployment/console-operator

# Restart console pods
oc delete pods -n openshift-console -l app=console

# Marketplace operator issues
oc get pods -n openshift-marketplace
oc logs -n openshift-marketplace deployment/marketplace-operator

# CatalogSource issues
oc describe catalogsource -n openshift-marketplace
```

## DISABLE DEVELOPER CATALOG
```bash
# Disable specific categories
oc patch console.operator.openshift.io/cluster --type json -p '[{"op":"remove","path":"/spec/customization/developerCatalog"}]'

# Disable samples operator
oc patch configs.samples.operator.openshift.io/cluster --type merge -p '{"spec":{"managementState":"Removed"}}'

# Delete catalog sources
oc delete catalogsource community-operators -n openshift-marketplace
```

## CONFIGURATION OPTIONS
```yaml
# Customize catalog categories
spec:
  customization:
    developerCatalog:
      categories:
      - id: custom
        label: Custom Apps
        tags: ["custom", "internal"]

# Filter operators
spec:
  operators:
    disabledDefaultSources:
    - certified-operators
    - community-operators

# Add custom catalog
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: my-catalog
  namespace: openshift-marketplace
spec:
  displayName: My Custom Catalog
  image: quay.io/myorg/catalog:latest
  publisher: MyOrg
  sourceType: grpc
```

## BEST PRACTICES
- Enable only needed catalog sources
- Create custom templates for common apps
- Use namespaces to organize templates
- Add quick starts for user guidance
- Monitor marketplace operator health
- Restrict operator installation with OperatorGroups
- Use custom catalogs for internal operators

## NOTES
- Developer Portal is part of web console
- Requires OperatorHub and Samples Operator
- Templates are namespace-scoped
- CatalogSources update automatically
- Quick starts provide guided tutorials
- Users need project creation permissions
