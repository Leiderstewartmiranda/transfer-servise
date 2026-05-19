# =====================================================
# Makefile — Transfer Service
# Uso: make <comando>
# =====================================================

.PHONY: help run test build sonar sonar-up sonar-down docker-up docker-down clean

# Colores
CYAN  = \033[0;36m
RESET = \033[0m

help: ## Muestra esta ayuda
	@echo ""
	@echo "  $(CYAN)Transfer Service — Comandos disponibles$(RESET)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-20s$(RESET) %s\n", $$1, $$2}'
	@echo ""

# ─── Desarrollo local ─────────────────────────────

run: ## Corre el servicio en modo local (H2 en memoria)
	mvn spring-boot:run

run-prod: ## Corre con perfil producción (requiere .env)
	export $$(cat .env | xargs) && mvn spring-boot:run -Dspring-boot.run.profiles=prod

test: ## Ejecuta todos los tests
	mvn test

test-coverage: ## Tests + reporte de cobertura JaCoCo
	mvn verify
	@echo "Reporte en: target/site/jacoco/index.html"

build: ## Compila el JAR (sin tests)
	mvn package -DskipTests

clean: ## Limpia el directorio target
	mvn clean

# ─── SonarQube ────────────────────────────────────

sonar-up: ## Levanta SonarQube y su BD (Docker)
	docker-compose up -d sonarqube sonar-db
	@echo "Esperando SonarQube en http://localhost:9000 (puede tardar ~60s)..."
	@echo "Login: admin / admin"

sonar-down: ## Detiene SonarQube
	docker-compose stop sonarqube sonar-db

sonar: ## Ejecuta análisis Sonar (requiere SONAR_TOKEN en .env)
	@[ -f .env ] && export $$(cat .env | xargs) || true
	mvn verify sonar:sonar \
		-Dsonar.token=$${SONAR_TOKEN} \
		-Dsonar.host.url=$${SONAR_HOST_URL:-http://localhost:9000}
	@echo "Ver resultados en: http://localhost:9000/dashboard?id=transfer-service"

sonar-all: sonar-up ## Levanta Sonar, corre tests y analiza
	@echo "Esperando 60s para que SonarQube esté listo..."
	@sleep 60
	$(MAKE) sonar

# ─── Docker stack completo ─────────────────────────

docker-build: ## Construye la imagen Docker del servicio
	docker build -t transfer-service:latest .

docker-up: ## Levanta el stack completo (App + Postgres + Sonar)
	@[ -f .env ] || (echo "ERROR: Crea el archivo .env desde .env.example" && exit 1)
	docker-compose up -d

docker-down: ## Detiene todos los contenedores
	docker-compose down

docker-logs: ## Ver logs del microservicio
	docker-compose logs -f app

docker-restart: ## Reinicia solo el microservicio
	docker-compose restart app

# ─── Utils ────────────────────────────────────────

login-admin: ## Obtiene JWT de admin (requiere curl y jq)
	@curl -s -X POST http://localhost:8080/api/v1/auth/login \
		-H "Content-Type: application/json" \
		-d '{"username":"admin","password":"admin123"}' | jq .

login-user: ## Obtiene JWT de user
	@curl -s -X POST http://localhost:8080/api/v1/auth/login \
		-H "Content-Type: application/json" \
		-d '{"username":"user","password":"user123"}' | jq .

health: ## Verifica el health del servicio
	@curl -s http://localhost:8080/actuator/health | jq .
