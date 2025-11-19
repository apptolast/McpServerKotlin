# Quick Start Guide - Docker CI/CD Workflow

## 🚀 Getting Started in 3 Steps

### Step 1: Verify Secrets (Already Done ✅)
The required secrets are already configured:
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

### Step 2: Push Code
```bash
git push origin main
# or
git push origin develop
```

### Step 3: Wait for Build
- Go to Actions tab on GitHub
- Watch the build progress
- Get kubectl commands from the summary

## 📊 What Gets Built

| Branch   | Docker Tags Created                              |
|----------|--------------------------------------------------|
| `main`   | `latest`, `main-<commit-sha>`                   |
| `develop`| `develop`, `develop-<commit-sha>`               |

## 🎯 Quick Commands

### View Workflow Status
```bash
# Visit: https://github.com/apptolast/McpServerKotlin/actions
```

### Update Kubernetes (After Build)
```bash
# Method 1: Using latest tag (for main branch)
kubectl set image deployment/mcp-fullstack-server \
  mcp-server=<username>/mcpserverkotlin:latest \
  -n cyberlab

# Method 2: Using develop tag
kubectl set image deployment/mcp-fullstack-server \
  mcp-server=<username>/mcpserverkotlin:develop \
  -n cyberlab

# Method 3: Restart (if using imagePullPolicy: Always)
kubectl rollout restart deployment/mcp-fullstack-server -n cyberlab

# Check rollout status
kubectl rollout status deployment/mcp-fullstack-server -n cyberlab
```

### Check Image on Docker Hub
```bash
# Visit: https://hub.docker.com/r/<username>/mcpserverkotlin/tags
```

## 🔍 Monitoring

### Add Build Badge to README
```markdown
![Docker Build](https://github.com/apptolast/McpServerKotlin/actions/workflows/docker-build-deploy.yml/badge.svg)
```

### View Recent Builds
1. Go to repository on GitHub
2. Click "Actions" tab
3. Select "Docker Build and Deploy"

## ⚡ Build Times

- **First build**: ~5-10 minutes
- **Cached builds**: ~2-5 minutes
- **Total**: ~7-12 minutes average

## 🐛 Common Issues

### Build Failed?
→ Check Actions logs for error details

### Image not pushed?
→ Verify Docker Hub credentials in Secrets

### Kubernetes not updating?
→ Run the kubectl commands from workflow summary

## 📚 Full Documentation

- Detailed guide: `.github/workflows/README.md`
- Implementation details: `.github/WORKFLOW_IMPLEMENTATION.md`

## 💡 Tips

✅ Always check the workflow summary for update commands
✅ Use image digests for production deployments
✅ Enable build notifications in GitHub settings
✅ Test in develop branch before merging to main

---

**Ready to deploy!** Just push your code and watch it build 🎉
