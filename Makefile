.PHONY: help build-backend test-backend run-backend clean-backend docker-up docker-down

help:
	@echo "Available commands:"
	@echo "  make build-backend    - Build the backend application"
	@echo "  make test-backend     - Run backend tests"
	@echo "  make run-backend      - Run the backend application"
	@echo "  make clean-backend    - Clean backend build artifacts"
	@echo "  make docker-up        - Start all services with Docker Compose"
	@echo "  make docker-down      - Stop all services"

build-backend:
	cd backend && ./gradlew build

test-backend:
	cd backend && ./gradlew test

run-backend:
	cd backend && ./gradlew bootRun

clean-backend:
	cd backend && ./gradlew clean

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down
