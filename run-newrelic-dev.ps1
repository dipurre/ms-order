# Script para ejecutar la aplicación con New Relic en modo DEV
# Uso: .\run-newrelic-dev.ps1

$ErrorActionPreference = "Stop"

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Ejecutando MS-Order con New Relic" -ForegroundColor Cyan
Write-Host "  Perfil: DEV" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que existe el JAR
$jarPath = "target\ms-order-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: No se encontró el JAR en $jarPath" -ForegroundColor Red
    Write-Host "Ejecuta primero: .\mvnw.cmd clean package" -ForegroundColor Yellow
    exit 1
}

# Verificar que existe el agente de New Relic
$newrelicAgent = "newrelic\newrelic-agent-8.25.1.jar"
if (-not (Test-Path $newrelicAgent)) {
    Write-Host "ERROR: No se encontró el agente de New Relic en $newrelicAgent" -ForegroundColor Red
    exit 1
}

# Configuración
$JAVA_OPTS = @(
    "-javaagent:$newrelicAgent",
    "-Dspring.profiles.active=dev",
    "-Dnewrelic.environment=dev",
    "-Dnewrelic.config.app_name=ms-order[dev]"
)

Write-Host "Configuración:" -ForegroundColor Green
Write-Host "  - JAR: $jarPath" -ForegroundColor Gray
Write-Host "  - New Relic Agent: $newrelicAgent" -ForegroundColor Gray
Write-Host "  - Perfil: dev" -ForegroundColor Gray
Write-Host "  - Environment: dev" -ForegroundColor Gray
Write-Host ""

# Ejecutar la aplicación
Write-Host "Iniciando aplicación..." -ForegroundColor Green
Write-Host ""

java $JAVA_OPTS -jar $jarPath

