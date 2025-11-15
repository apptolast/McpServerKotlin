#!/bin/bash
# MCP Full-Stack Server Deployment Script

set -e

echo "🚀 MCP Full-Stack Server Deployment"
echo "======================================"

# Configuration
VERSION=${VERSION:-"1.0.0"}
NAMESPACE=${NAMESPACE:-"cyberlab"}
REGISTRY=${REGISTRY:-"apptolast.com"}
IMAGE_NAME="mcp-fullstack-server"

# Build application
echo ""
echo "📦 Building application..."
./gradlew clean build -x test

# Build Docker image
echo ""
echo "🐳 Building Docker image..."
docker build -t ${REGISTRY}/${IMAGE_NAME}:${VERSION} -f docker/Dockerfile .
docker tag ${REGISTRY}/${IMAGE_NAME}:${VERSION} ${REGISTRY}/${IMAGE_NAME}:latest

# Push to registry (optional, if registry is configured)
if [ "$PUSH_TO_REGISTRY" = "true" ]; then
    echo ""
    echo "📤 Pushing to registry..."
    docker push ${REGISTRY}/${IMAGE_NAME}:${VERSION}
    docker push ${REGISTRY}/${IMAGE_NAME}:latest
fi

# Deploy to Kubernetes (optional)
if [ "$DEPLOY_TO_K8S" = "true" ]; then
    echo ""
    echo "☸️  Deploying to Kubernetes..."
    
    # Create namespace if it doesn't exist
    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    
    # Apply configurations
    kubectl apply -f k8s/configmap.yaml
    kubectl apply -f k8s/secrets.yaml
    kubectl apply -f k8s/rbac.yaml
    kubectl apply -f k8s/deployment.yaml
    kubectl apply -f k8s/service.yaml
    
    # Wait for deployment
    echo "⏳ Waiting for deployment to be ready..."
    kubectl wait --for=condition=available --timeout=300s \
        deployment/mcp-fullstack-server -n ${NAMESPACE} || true
    
    # Get service info
    echo ""
    echo "📊 Service Information:"
    kubectl get svc mcp-fullstack-server -n ${NAMESPACE}
    
    echo ""
    echo "✅ Deployment completed!"
    echo ""
    echo "📝 To check logs:"
    echo "kubectl logs -f deployment/mcp-fullstack-server -n ${NAMESPACE}"
    echo ""
    echo "🔗 To access the server locally:"
    echo "kubectl port-forward svc/mcp-fullstack-server 3000:3000 -n ${NAMESPACE}"
else
    echo ""
    echo "✅ Build completed!"
    echo ""
    echo "To run locally:"
    echo "  java -jar build/libs/mcp-fullstack-server-1.0.0.jar"
    echo ""
    echo "To run with Docker:"
    echo "  docker run -p 3000:3000 ${REGISTRY}/${IMAGE_NAME}:${VERSION}"
fi

echo ""
echo "🎉 Done!"
