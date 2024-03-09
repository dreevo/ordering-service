# Build
custom_build(
     # Name of the container image
     ref = 'order-service',
     tag = 'latest',
     # Command to build the container image
     command = '.\\gradlew.bat bootBuildImage --imageName order-service:latest',
     # Files to watch that trigger a new build
     deps = ['build.gradle', 'src']
)
# Deploy
k8s_yaml(kustomize('k8s'))
# Manage
k8s_resource('order-service', port_forwards=['9002'])