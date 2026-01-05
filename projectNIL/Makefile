# ProjectNIL Makefile
# Basic commands for local development

COMPOSE_FILE := podman-compose.yaml

# Database connection settings
DB_CONTAINER := projectnil-db
DB_NAME := projectnil
DB_USER := projectnil

.PHONY: help setup psql api compiler clean test check build

help:
	@echo "ProjectNIL Development Commands"
	@echo "================================"
	@echo "  make setup     - Start infrastructure (postgres) + run migrations" 
	@echo "  make psql      - Connect to the database with psql"
	@echo "  make api       - Run the API service (bootRun)"
	@echo "  make compiler  - Run the Compiler service (bootRun)"
	@echo "  make clean     - Stop and remove containers"
	@echo "  make test      - Run tests"
	@echo "  make check     - Run checks (lint, formatting, etc.)"
	@echo "  make build     - Build the project"

# Start infrastructure services (postgres + migrations)
setup:
	podman compose -f $(COMPOSE_FILE) up -d

# Connect to the database with psql
psql:
	podman exec -it $(DB_CONTAINER) psql -U $(DB_USER) -d $(DB_NAME)

# Run the API service
api:
	./gradlew :services:api:bootRun

# Run the Compiler service
compiler:
	./gradlew :services:compiler:bootRun


# Stop and remove all containers and volumes
clean:
	podman compose -f $(COMPOSE_FILE) down -v

test:
	./gradlew test

check:
	./gradlew check

build:
	./gradlew build
