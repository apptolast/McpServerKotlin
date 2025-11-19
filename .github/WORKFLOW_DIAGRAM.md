# GitHub Actions CI/CD Workflow Diagram

## High-Level Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                      Developer Workflow                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ git push origin main/develop
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      GitHub Repository                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Trigger Conditions:                                     │  │
│  │  - Push to main branch    → Tag: latest, main-<sha>     │  │
│  │  - Push to develop branch → Tag: develop, develop-<sha> │  │
│  │  - Ignore: *.md, docs/, LICENSE                         │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              GitHub Actions Workflow Execution                  │
│                                                                 │
│  Job 1: build-and-push                                         │
│  ┌───────────────────────────────────────────────────────┐    │
│  │ Step 1: Checkout Code                                 │    │
│  │   └─ actions/checkout@v4                             │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 2: Setup Docker Buildx                           │    │
│  │   └─ docker/setup-buildx-action@v3                   │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 3: Login to Docker Hub                           │    │
│  │   └─ docker/login-action@v3                          │    │
│  │   └─ Uses: DOCKERHUB_USERNAME, DOCKERHUB_TOKEN       │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 4: Extract Metadata                              │    │
│  │   └─ docker/metadata-action@v5                       │    │
│  │   └─ Generates: tags, labels, version                │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 5: Build and Push Image                          │    │
│  │   └─ docker/build-push-action@v5                     │    │
│  │   └─ Context: .                                       │    │
│  │   └─ Dockerfile: ./docker/Dockerfile                 │    │
│  │   └─ Push: true                                       │    │
│  │   └─ Cache: GitHub Actions cache                     │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 6: Generate Build Summary                        │    │
│  │   └─ Display: tags, digest, commit info              │    │
│  └───────────────────────────────────────────────────────┘    │
│                             │                                   │
│                             │ Outputs:                          │
│                             │ - image-digest                    │
│                             │ - image-tags                      │
│                             │ - image-version                   │
│                             ▼                                   │
│  Job 2: update-kubernetes (depends on job 1)                   │
│  ┌───────────────────────────────────────────────────────┐    │
│  │ Step 1: Checkout Repository                           │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 2: Create Deployment Signal                      │    │
│  │   └─ File: .deployment/latest-deploy.txt             │    │
│  │   └─ Contains: image, digest, timestamp, commit      │    │
│  ├───────────────────────────────────────────────────────┤    │
│  │ Step 3: Generate kubectl Commands                     │    │
│  │   └─ Display in workflow summary                     │    │
│  │   └─ Method 1: Using image digest (recommended)      │    │
│  │   └─ Method 2: Using tag (latest/develop)            │    │
│  │   └─ Method 3: Rollout restart                       │    │
│  └───────────────────────────────────────────────────────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Hub                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Repository: <username>/mcpserverkotlin                   │  │
│  │                                                          │  │
│  │ Tags Created (main branch):                             │  │
│  │   - latest                                              │  │
│  │   - main-a1b2c3d                                        │  │
│  │                                                          │  │
│  │ Tags Created (develop branch):                          │  │
│  │   - develop                                             │  │
│  │   - develop-a1b2c3d                                     │  │
│  │                                                          │  │
│  │ Metadata:                                               │  │
│  │   - Image digest (SHA256)                               │  │
│  │   - OCI labels (title, description, source, etc.)       │  │
│  │   - Build timestamp                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ Manual kubectl command
                             │ (from workflow summary)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Namespace: cyberlab                                      │  │
│  │ Deployment: mcp-fullstack-server                         │  │
│  │                                                          │  │
│  │ Update Commands:                                         │  │
│  │                                                          │  │
│  │ 1. kubectl set image deployment/mcp-fullstack-server \ │  │
│  │      mcp-server=<user>/mcpserverkotlin@sha256:... \    │  │
│  │      -n cyberlab                                        │  │
│  │                                                          │  │
│  │ 2. kubectl rollout status deployment/... -n cyberlab    │  │
│  │                                                          │  │
│  │ Result:                                                  │  │
│  │   - Pods restart with new image                         │  │
│  │   - Rolling update ensures zero downtime                │  │
│  │   - Health checks validate new pods                     │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Detailed Workflow Steps

### Phase 1: Trigger Detection
```
Git Push
   │
   ├─ Branch Check
   │    ├─ main → Continue
   │    ├─ develop → Continue
   │    └─ other → Skip
   │
   └─ Path Check
        ├─ Code files → Continue
        └─ Only docs → Skip
```

### Phase 2: Docker Build
```
Build Process
   │
   ├─ Gradle Dependencies
   │    └─ Cached from GitHub Actions
   │
   ├─ Compile Kotlin Code
   │    └─ JDK 21, Kotlin 2.2.0
   │
   ├─ Create JAR
   │    └─ build/libs/*.jar
   │
   └─ Multi-stage Dockerfile
        ├─ Stage 1: Builder (gradle:8.10-jdk21)
        │    ├─ Copy gradle files
        │    ├─ Download dependencies
        │    ├─ Copy source code
        │    └─ Build application
        │
        └─ Stage 2: Runtime (eclipse-temurin:21-jre-alpine)
             ├─ Install tools (bash, git, curl, jq)
             ├─ Create non-root user
             ├─ Copy JAR from builder
             ├─ Set up workspace
             └─ Configure entrypoint
```

### Phase 3: Image Tagging
```
Tag Strategy
   │
   ├─ Branch: main
   │    ├─ latest
   │    └─ main-<short-sha>
   │
   ├─ Branch: develop
   │    ├─ develop
   │    └─ develop-<short-sha>
   │
   └─ Future: Semantic Versioning
        ├─ v1.2.3 (on tag push)
        └─ v1.2 (on tag push)
```

### Phase 4: Kubernetes Update
```
Update Options
   │
   ├─ Option 1: Image Digest (Recommended)
   │    └─ kubectl set image ... @sha256:abc123...
   │    └─ Guarantees exact version
   │    └─ Immutable reference
   │
   ├─ Option 2: Tag-based
   │    └─ kubectl set image ... :latest
   │    └─ Simple, easy to remember
   │    └─ May pull wrong version if cached
   │
   └─ Option 3: Rollout Restart
        └─ kubectl rollout restart ...
        └─ Works with imagePullPolicy: Always
        └─ Forces pod recreation
```

## Build Performance

```
Build Timeline
   │
   ├─ First Build (No Cache)
   │    ├─ Checkout: ~10s
   │    ├─ Setup: ~30s
   │    ├─ Gradle Dependencies: ~2-3 min
   │    ├─ Compilation: ~1-2 min
   │    ├─ Docker Build: ~2-3 min
   │    └─ Push: ~1-2 min
   │    └─ Total: ~5-10 minutes
   │
   └─ Cached Build
        ├─ Checkout: ~10s
        ├─ Setup: ~30s
        ├─ Gradle Dependencies: ~30s (cached)
        ├─ Compilation: ~30s (cached)
        ├─ Docker Build: ~1 min (cached layers)
        └─ Push: ~1 min
        └─ Total: ~2-5 minutes
```

## Security Flow

```
Security Layers
   │
   ├─ GitHub Secrets
   │    ├─ DOCKERHUB_USERNAME (encrypted)
   │    └─ DOCKERHUB_TOKEN (encrypted)
   │
   ├─ Workflow Permissions
   │    ├─ contents: read
   │    └─ packages: write
   │
   ├─ Docker Hub Authentication
   │    └─ Token-based (no password)
   │
   ├─ Image Signing (Future)
   │    └─ Cosign integration
   │
   └─ Vulnerability Scanning (Future)
        └─ Trivy integration
```

## Monitoring & Observability

```
Monitoring Points
   │
   ├─ GitHub Actions UI
   │    ├─ Workflow run status
   │    ├─ Build logs
   │    ├─ Job duration
   │    └─ Build summary
   │
   ├─ Docker Hub
   │    ├─ Image tags
   │    ├─ Image size
   │    ├─ Pull statistics
   │    └─ Vulnerability scans
   │
   └─ Kubernetes
        ├─ Pod status
        ├─ Deployment events
        ├─ Container logs
        └─ Health checks
```

## Error Handling

```
Error Scenarios
   │
   ├─ Build Failure
   │    ├─ Compilation error
   │    ├─ Dependency issue
   │    └─ Action: Check build logs
   │
   ├─ Authentication Failure
   │    ├─ Invalid credentials
   │    ├─ Expired token
   │    └─ Action: Update secrets
   │
   ├─ Push Failure
   │    ├─ Network issue
   │    ├─ Rate limit
   │    └─ Action: Retry or wait
   │
   └─ Kubernetes Update Failure
        ├─ Wrong namespace
        ├─ Permission issue
        └─ Action: Verify kubectl config
```

---

**Last Updated**: November 2024
**Version**: 1.0.0
