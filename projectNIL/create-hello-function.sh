#!/bin/bash
# Creates a simple "hello world" function that outputs "Hello, $name!"
#
# Usage:
#   ./create-hello-function.sh [API_URL]
#
# Examples:
#   ./create-hello-function.sh                    # Uses http://localhost:8080
#   ./create-hello-function.sh http://server:8080 # Uses custom URL
#
# After creation, execute with:
#   curl -X POST $API_URL/functions/$FUNCTION_ID/execute \
#     -H "Content-Type: application/json" \
#     -d '{"input": "World"}'

set -e

API_URL="${1:-http://localhost:8080}"

echo "Creating hello function on $API_URL..."

# AssemblyScript source that outputs "Hello, $input!"
# Takes a string input and returns a greeting string
# Note: Pure AssemblyScript has no JSON.parse; input is passed as-is
SOURCE='export function handle(input: string): string {
  return "Hello, " + input + "!";
}'

# Create the function
RESPONSE=$(curl -s -X POST "$API_URL/functions" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"hello\",
    \"description\": \"A simple function that says hello to the given name\",
    \"language\": \"assemblyscript\",
    \"source\": $(echo "$SOURCE" | jq -Rs .)
  }")

echo "Response:"
echo "$RESPONSE" | jq .

# Extract function ID
FUNCTION_ID=$(echo "$RESPONSE" | jq -r '.id')

if [ "$FUNCTION_ID" == "null" ] || [ -z "$FUNCTION_ID" ]; then
  echo "Error: Failed to create function"
  exit 1
fi

echo ""
echo "Function created with ID: $FUNCTION_ID"
echo "Status: $(echo "$RESPONSE" | jq -r '.status')"
echo ""
echo "Wait for compilation to complete, then execute with:"
echo ""
echo "  # Check status (wait for READY)"
echo "  curl $API_URL/functions/$FUNCTION_ID"
echo ""
echo "  # Execute the function"
echo "  curl -X POST $API_URL/functions/$FUNCTION_ID/execute \\"
echo "    -H \"Content-Type: application/json\" \\"
echo "    -d '{\"input\": \"YourName\"}'"
echo ""
echo "Expected output: Hello, YourName!"
