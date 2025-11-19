#!/bin/bash
# GitHub Actions Workflow Validation Script
# This script validates that the workflow is correctly configured

set -e

echo "🔍 GitHub Actions Workflow Validation"
echo "======================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track validation status
ALL_CHECKS_PASSED=true

# Function to print status
check_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        ALL_CHECKS_PASSED=false
    fi
}

# Check 1: Workflow file exists
echo "1️⃣  Checking workflow file..."
if [ -f ".github/workflows/docker-build-deploy.yml" ]; then
    check_status 0 "Workflow file exists"
else
    check_status 1 "Workflow file missing"
fi
echo ""

# Check 2: Validate YAML syntax
# Prerequisites: Python 3 with PyYAML library installed
# Install with: pip3 install pyyaml
echo "2️⃣  Validating YAML syntax..."
if python3 -c "import yaml; yaml.safe_load(open('.github/workflows/docker-build-deploy.yml'))" 2>/dev/null; then
    check_status 0 "YAML syntax is valid"
else
    check_status 1 "YAML syntax error (requires Python 3 with PyYAML: pip3 install pyyaml)"
fi
echo ""

# Check 3: Check for required workflow components
echo "3️⃣  Checking workflow components..."

# Check for triggers
if grep -q "on:" ".github/workflows/docker-build-deploy.yml" && \
   grep -q "main" ".github/workflows/docker-build-deploy.yml" && \
   grep -q "develop" ".github/workflows/docker-build-deploy.yml"; then
    check_status 0 "Workflow triggers configured (main, develop)"
else
    check_status 1 "Workflow triggers missing or incomplete"
fi

# Check for Docker Hub login
if grep -q "docker/login-action" ".github/workflows/docker-build-deploy.yml"; then
    check_status 0 "Docker Hub login action configured"
else
    check_status 1 "Docker Hub login action missing"
fi

# Check for Docker build and push
if grep -q "docker/build-push-action" ".github/workflows/docker-build-deploy.yml"; then
    check_status 0 "Docker build and push action configured"
else
    check_status 1 "Docker build and push action missing"
fi

# Check for secrets usage
if grep -q "DOCKERHUB_USERNAME" ".github/workflows/docker-build-deploy.yml" && \
   grep -q "DOCKERHUB_TOKEN" ".github/workflows/docker-build-deploy.yml"; then
    check_status 0 "Docker Hub secrets configured"
else
    check_status 1 "Docker Hub secrets not configured"
fi

echo ""

# Check 4: Dockerfile exists
echo "4️⃣  Checking Dockerfile..."
if [ -f "docker/Dockerfile" ]; then
    check_status 0 "Dockerfile exists at docker/Dockerfile"
else
    check_status 1 "Dockerfile missing"
fi
echo ""

# Check 5: Documentation exists
echo "5️⃣  Checking documentation..."
if [ -f ".github/workflows/README.md" ]; then
    check_status 0 "Workflow documentation exists"
else
    check_status 1 "Workflow documentation missing"
fi

if [ -f ".github/WORKFLOW_IMPLEMENTATION.md" ]; then
    check_status 0 "Implementation guide exists"
else
    check_status 1 "Implementation guide missing"
fi

if [ -f ".github/QUICKSTART.md" ]; then
    check_status 0 "Quick start guide exists"
else
    check_status 1 "Quick start guide missing"
fi
echo ""

# Check 6: Git repository status
echo "6️⃣  Checking git repository..."
if git rev-parse --git-dir > /dev/null 2>&1; then
    check_status 0 "Git repository initialized"
    
    # Check if workflow files are tracked
    if git ls-files --error-unmatch .github/workflows/docker-build-deploy.yml > /dev/null 2>&1; then
        check_status 0 "Workflow file is tracked by git"
    else
        echo -e "${YELLOW}⚠${NC} Workflow file not yet committed"
    fi
else
    check_status 1 "Not a git repository"
fi
echo ""

# Check 7: Validate Docker Hub repository name
echo "7️⃣  Checking Docker Hub configuration..."
DOCKER_REPO=$(grep "DOCKER_REPOSITORY:" ".github/workflows/docker-build-deploy.yml" | cut -d: -f2 | xargs)
if [ ! -z "$DOCKER_REPO" ]; then
    check_status 0 "Docker repository name: $DOCKER_REPO"
else
    check_status 1 "Docker repository name not found"
fi
echo ""

# Check 8: Verify Kubernetes deployment configuration
echo "8️⃣  Checking Kubernetes deployment files..."
if [ -f "k8s/deployment.yaml" ]; then
    check_status 0 "Kubernetes deployment manifest exists"
    
    # Check for deployment name
    if grep -q "mcp-fullstack-server" "k8s/deployment.yaml"; then
        check_status 0 "Deployment name matches workflow"
    else
        echo -e "${YELLOW}⚠${NC} Deployment name might not match"
    fi
    
    # Check for namespace
    if grep -q "cyberlab" "k8s/deployment.yaml"; then
        check_status 0 "Namespace matches workflow (cyberlab)"
    else
        echo -e "${YELLOW}⚠${NC} Namespace might not match"
    fi
else
    echo -e "${YELLOW}⚠${NC} Kubernetes deployment manifest not found (optional)"
fi
echo ""

# Summary
echo "======================================"
echo "📋 Validation Summary"
echo "======================================"
if $ALL_CHECKS_PASSED; then
    echo -e "${GREEN}✓ All critical checks passed!${NC}"
    echo ""
    echo "🎉 Your workflow is ready to use!"
    echo ""
    echo "Next steps:"
    echo "1. Verify GitHub secrets are configured:"
    echo "   - DOCKERHUB_USERNAME"
    echo "   - DOCKERHUB_TOKEN"
    echo ""
    echo "2. Commit and push the workflow:"
    echo "   git add .github/"
    echo "   git commit -m 'Add Docker CI/CD workflow'"
    echo "   git push origin main"
    echo ""
    echo "3. Check the Actions tab on GitHub to see the workflow run"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some checks failed${NC}"
    echo ""
    echo "Please review the errors above and fix them before proceeding."
    echo ""
    echo "For help, see:"
    echo "- .github/workflows/README.md"
    echo "- .github/WORKFLOW_IMPLEMENTATION.md"
    echo ""
    exit 1
fi
