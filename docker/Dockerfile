# Build stage
FROM gradle:8.10-jdk21 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Download dependencies
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build application
RUN gradle clean build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required tools
RUN apk add --no-cache \
    bash \
    git \
    curl \
    jq

# Create non-root user
RUN addgroup -g 1000 mcp && \
    adduser -u 1000 -G mcp -s /bin/bash -D mcp

# Copy built application
COPY --from=builder /app/build/libs/*.jar ./app.jar

# Create workspace directories
RUN mkdir -p /workspace /workspace/.mcp-memory /workspace/resources && \
    chown -R mcp:mcp /workspace /app

USER mcp

# Environment variables
ENV MCP_HOST=0.0.0.0
ENV MCP_PORT=3000
ENV MCP_WORKING_DIR=/workspace

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]
