# GitHub Actions Workflow Documentation

## Docker Build and Deploy Workflow

This workflow automates the building and deployment of Docker images for the MCP Full-Stack Server.

### Workflow File

`.github/workflows/docker-build-deploy.yml`

### Triggers

The workflow is triggered on:
- Push to `main` branch
- Push to `develop` branch

The workflow **ignores** changes to:
- Markdown files (`**.md`)
- Documentation folder (`docs/**`)
- `.gitignore` file
- `LICENSE` file

### Workflow Jobs

#### 1. Build and Push (`build-and-push`)

This job builds the Docker image and pushes it to Docker Hub.

**Steps:**
1. **Checkout repository** - Uses `actions/checkout@v4`
2. **Set up Docker Buildx** - Enables advanced Docker build features
3. **Log in to Docker Hub** - Authenticates using secrets
4. **Extract Docker metadata** - Generates tags and labels
5. **Build and push Docker image** - Builds and pushes the image
6. **Display build summary** - Shows build information

**Outputs:**
- `image-digest`: The SHA256 digest of the pushed image
- `image-tags`: All tags applied to the image
- `image-version`: Semantic version (if applicable)

**Docker Image Tags:**
- `latest` - For pushes to the `main` branch
- `develop` - For pushes to the `develop` branch
- `<branch>-<short-sha>` - For all branches (e.g., `main-a1b2c3d`)
- `<version>` - If a semantic version tag is pushed (future-proof)

#### 2. Update Kubernetes (`update-kubernetes`)

This job provides instructions and mechanisms to update Kubernetes deployments.

**Steps:**
1. **Checkout repository** - Gets deployment scripts if needed
2. **Create deployment signal** - Creates a timestamped deployment record
3. **Display Kubernetes update summary** - Provides manual update commands

**Kubernetes Update Methods:**

**Method 1: Using Image Digest (Recommended)**
```bash
kubectl set image deployment/mcp-fullstack-server \
  mcp-server=<username>/mcpserverkotlin@<digest> \
  -n cyberlab

kubectl rollout status deployment/mcp-fullstack-server -n cyberlab
```

**Method 2: Using Tag**
```bash
# For main branch
kubectl set image deployment/mcp-fullstack-server \
  mcp-server=<username>/mcpserverkotlin:latest \
  -n cyberlab

# For develop branch
kubectl set image deployment/mcp-fullstack-server \
  mcp-server=<username>/mcpserverkotlin:develop \
  -n cyberlab
```

**Method 3: Restart Deployment**
```bash
# If using imagePullPolicy: Always
kubectl rollout restart deployment/mcp-fullstack-server -n cyberlab
```

### Required Secrets

These secrets must be configured in the GitHub repository settings:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `DOCKERHUB_USERNAME` | Docker Hub username | `myusername` |
| `DOCKERHUB_TOKEN` | Docker Hub access token | `dckr_pat_...` |

**How to configure secrets:**
1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add each secret with the appropriate value

**Creating a Docker Hub Access Token:**
1. Log in to [Docker Hub](https://hub.docker.com/)
2. Go to Account Settings → Security
3. Click "New Access Token"
4. Give it a descriptive name (e.g., "GitHub Actions")
5. Copy the token and add it as `DOCKERHUB_TOKEN` secret

### Environment Variables

The workflow uses these environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DOCKER_REPOSITORY` | Docker Hub repository name | `mcpserverkotlin` |
| `IMAGE_NAME` | Full image name | `$DOCKERHUB_USERNAME/mcpserverkotlin` |

### Build Cache

The workflow uses GitHub Actions cache to speed up Docker builds:
- `cache-from: type=gha` - Read from GitHub Actions cache
- `cache-to: type=gha,mode=max` - Write to GitHub Actions cache (maximized)

This significantly reduces build times for subsequent runs.

### Workflow Outputs

The workflow generates:
1. **Build Summary** - In the Actions run summary (visible in GitHub UI)
2. **Deployment Signal** - File with deployment metadata (`.deployment/latest-deploy.txt`)
3. **Kubernetes Update Commands** - Manual commands to update the deployment

### Docker Image Metadata

Each image includes OCI-compliant labels:
- `org.opencontainers.image.title` - "MCP Full-Stack Server"
- `org.opencontainers.image.description` - Project description
- `org.opencontainers.image.vendor` - "AppToLast"
- `org.opencontainers.image.source` - GitHub repository URL
- `org.opencontainers.image.revision` - Git commit SHA
- `org.opencontainers.image.created` - Build timestamp

### Kubernetes Integration

#### Option 1: Manual Update (Current Implementation)

After the workflow completes, manually run the kubectl commands provided in the workflow summary.

#### Option 2: Automatic Update (Future Enhancement)

To enable automatic Kubernetes updates, you can:

1. **Use a webhook trigger**:
   - Deploy a webhook receiver in your cluster
   - Add the webhook URL as a secret: `K8S_WEBHOOK_URL`
   - Uncomment the webhook step in the workflow

2. **Use a GitOps tool**:
   - ArgoCD
   - Flux CD
   - Configure image update automation

3. **Use imagePullPolicy: Always**:
   - Update the Kubernetes deployment to use `imagePullPolicy: Always`
   - Use `kubectl rollout restart` to pull the latest image

#### Option 3: Image Pull Policy Configuration

Update your Kubernetes deployment to automatically pull new images:

```yaml
spec:
  template:
    spec:
      containers:
      - name: mcp-server
        image: username/mcpserverkotlin:latest
        imagePullPolicy: Always  # Always pull the latest image
```

Then, you can simply restart the deployment:
```bash
kubectl rollout restart deployment/mcp-fullstack-server -n cyberlab
```

### Monitoring Workflow Runs

**View workflow runs:**
1. Go to the repository on GitHub
2. Click the "Actions" tab
3. Select "Docker Build and Deploy" workflow
4. View individual run details, logs, and summaries

**Check build status:**
- Build status badge can be added to README.md:
  ```markdown
  ![Docker Build](https://github.com/apptolast/McpServerKotlin/actions/workflows/docker-build-deploy.yml/badge.svg)
  ```

### Troubleshooting

#### Authentication Failed
- Verify `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets are correctly set
- Ensure the access token has write permissions
- Check if the token has expired

#### Build Failed
- Review the build logs in the Actions tab
- Check if the Dockerfile is valid
- Verify all dependencies are accessible

#### Image Push Failed
- Ensure the Docker Hub repository exists
- Verify repository permissions
- Check Docker Hub rate limits

#### Kubernetes Update Not Working
- Verify kubectl context is correct
- Check namespace exists (`cyberlab`)
- Ensure deployment name is correct (`mcp-fullstack-server`)
- Verify image name matches the pushed image

### Best Practices

1. **Use Image Digests**: For production, use image digests instead of tags for guaranteed consistency
2. **Monitor Workflow Runs**: Set up notifications for failed workflow runs
3. **Review Build Summaries**: Check the workflow summary after each run
4. **Tag Releases**: Use semantic versioning tags for releases
5. **Test Before Deploying**: Test images in staging before updating production

### Version History

- **v1.0.0**: Initial workflow implementation
  - Docker build and push to Docker Hub
  - Multi-branch support (main/develop)
  - Build cache optimization
  - Kubernetes update instructions

### References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Build and Push Action](https://github.com/docker/build-push-action)
- [Docker Metadata Action](https://github.com/docker/metadata-action)
- [Kubernetes Deployment Updates](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#updating-a-deployment)

### Support

For issues or questions about this workflow:
1. Check the workflow logs in the Actions tab
2. Review this documentation
3. Create an issue in the repository
4. Contact the DevOps team

---

**Last Updated**: November 2024
**Maintained By**: AppToLast DevOps Team
