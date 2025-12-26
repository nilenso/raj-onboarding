#!/bin/sh
# Helper script to run Gradle commands with Podman configured for Testcontainers
set -euo pipefail

# Check if podman is available
if ! command -v podman >/dev/null 2>&1; then
  echo "Error: podman is not installed" >&2
  exit 1
fi

# Check if podman machine is running
if ! podman machine list --format "{{.Running}}" 2>/dev/null | grep -q "true"; then
  echo "Starting podman machine..."
  podman machine start
fi

# Get the socket path from podman machine inspect
SOCKET_PATH=$(podman machine inspect --format "{{.ConnectionInfo.PodmanSocket.Path}}" 2>/dev/null)

if [ -z "$SOCKET_PATH" ] || [ ! -S "$SOCKET_PATH" ]; then
  echo "Error: Could not find podman socket at $SOCKET_PATH" >&2
  exit 1
fi

export DOCKER_HOST="unix://$SOCKET_PATH"
export TESTCONTAINERS_RYUK_DISABLED=true

echo "Using Podman socket: $DOCKER_HOST"
exec "$@"
