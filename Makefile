SHELL := /bin/sh

BACKEND_POM := backend/pom.xml
FRONTEND_DIR := frontend
FRONTEND_STAMP := $(FRONTEND_DIR)/node_modules/.package-lock.stamp
DOCKER_COMPOSE := docker compose -f backend/docker-compose.yml
POSTGRES_CONTAINER := mindful-finance-postgres
# Shared local defaults stay overridable via exported env vars.
SPRING_PROFILES_ACTIVE ?= postgres
MINDFUL_FINANCE_DB_URL ?= jdbc:postgresql://localhost:55432/mindfulfinance
MINDFUL_FINANCE_DB_USERNAME ?= mindfulfinance
MINDFUL_FINANCE_DB_PASSWORD ?= mindfulfinance
BACKEND_ENV := SPRING_PROFILES_ACTIVE=$(SPRING_PROFILES_ACTIVE) MINDFUL_FINANCE_DB_URL=$(MINDFUL_FINANCE_DB_URL) MINDFUL_FINANCE_DB_USERNAME=$(MINDFUL_FINANCE_DB_USERNAME) MINDFUL_FINANCE_DB_PASSWORD=$(MINDFUL_FINANCE_DB_PASSWORD)
BACKEND_PREPARE_CMD := mvn -f $(BACKEND_POM) -pl api -am -Dmaven.test.skip=true install
BACKEND_DEV_CMD := $(BACKEND_ENV) mvn -f $(BACKEND_POM) -pl api -am -rf :api spring-boot:run
BACKEND_BUILD_CMD := mvn -f $(BACKEND_POM) -Dmaven.test.skip=true package
FRONTEND_DEV_CMD := npm --prefix $(FRONTEND_DIR) run dev
FRONTEND_BUILD_CMD := npm --prefix $(FRONTEND_DIR) run build

.PHONY: dev build down frontend-deps db-up backend-dev

$(FRONTEND_STAMP): $(FRONTEND_DIR)/package.json $(FRONTEND_DIR)/package-lock.json
	@echo "Installing frontend dependencies..."
	@npm --prefix $(FRONTEND_DIR) ci
	@mkdir -p $(dir $(FRONTEND_STAMP))
	@touch $(FRONTEND_STAMP)

frontend-deps: $(FRONTEND_STAMP)

build: $(FRONTEND_STAMP)
	@echo "Building backend..."
	@$(BACKEND_BUILD_CMD)
	@echo "Building frontend..."
	@$(FRONTEND_BUILD_CMD)

down:
	@$(DOCKER_COMPOSE) down

db-up:
	@set -eu; \
	echo "Starting PostgreSQL..."; \
	$(DOCKER_COMPOSE) up -d postgres; \
	echo "Waiting for PostgreSQL healthcheck..."; \
	attempt=0; \
	until [ "$$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $(POSTGRES_CONTAINER) 2>/dev/null || echo missing)" = "healthy" ]; do \
		attempt=$$((attempt + 1)); \
		if [ "$$attempt" -ge 30 ]; then \
			echo "PostgreSQL did not become healthy in time."; \
			exit 1; \
		fi; \
		sleep 2; \
	done; \
	echo "PostgreSQL is healthy."

backend-dev:
	@echo "Preparing backend modules..."
	@$(BACKEND_PREPARE_CMD)
	@$(MAKE) db-up
	@echo "Starting backend on http://localhost:8080 ..."
	@$(BACKEND_DEV_CMD)

dev: $(FRONTEND_STAMP)
	@set -eu; \
	backend_pid=""; \
	frontend_pid=""; \
	cleanup() { \
		status="$$1"; \
		if [ -n "$$backend_pid" ] && kill -0 "$$backend_pid" 2>/dev/null; then \
			kill "$$backend_pid" 2>/dev/null || true; \
		fi; \
		if [ -n "$$frontend_pid" ] && kill -0 "$$frontend_pid" 2>/dev/null; then \
			kill "$$frontend_pid" 2>/dev/null || true; \
		fi; \
		if [ -n "$$backend_pid" ]; then \
			wait "$$backend_pid" 2>/dev/null || true; \
		fi; \
		if [ -n "$$frontend_pid" ]; then \
			wait "$$frontend_pid" 2>/dev/null || true; \
		fi; \
		exit "$$status"; \
	}; \
	trap 'cleanup 130' INT TERM; \
	echo "Preparing backend modules..."; \
	$(BACKEND_PREPARE_CMD); \
	$(MAKE) db-up; \
	echo "Starting backend on http://localhost:8080 ..."; \
	$(BACKEND_DEV_CMD) & \
	backend_pid=$$!; \
	echo "Starting frontend on http://localhost:5173 ..."; \
	$(FRONTEND_DEV_CMD) & \
	frontend_pid=$$!; \
	echo "Dev stack is running. Press Ctrl+C to stop app processes; PostgreSQL stays up."; \
	while :; do \
		if ! kill -0 "$$backend_pid" 2>/dev/null; then \
			if wait "$$backend_pid"; then \
				backend_status=0; \
			else \
				backend_status=$$?; \
			fi; \
			echo "Backend exited."; \
			if kill -0 "$$frontend_pid" 2>/dev/null; then \
				kill "$$frontend_pid" 2>/dev/null || true; \
			fi; \
			wait "$$frontend_pid" 2>/dev/null || true; \
			exit "$$backend_status"; \
		fi; \
		if ! kill -0 "$$frontend_pid" 2>/dev/null; then \
			if wait "$$frontend_pid"; then \
				frontend_status=0; \
			else \
				frontend_status=$$?; \
			fi; \
			echo "Frontend exited."; \
			if kill -0 "$$backend_pid" 2>/dev/null; then \
				kill "$$backend_pid" 2>/dev/null || true; \
			fi; \
			wait "$$backend_pid" 2>/dev/null || true; \
			exit "$$frontend_status"; \
		fi; \
		sleep 1; \
	done
