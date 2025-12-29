# Script para ejecutar la aplicación con New Relic Agent

# Configurar variables de entorno
$env:NEW_RELIC_LICENSE_KEY="TU_LICENSE_KEY_AQUI"
$env:NEW_RELIC_APP_NAME="ms-product"

# Descargar el agente de New Relic si no existe
if (-not (Test-Path "newrelic/newrelic.jar")) {
    Write-Host "Descargando New Relic Java Agent..."
    New-Item -ItemType Directory -Force -Path newrelic
    Invoke-WebRequest -Uri "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip" -OutFile "newrelic.zip"
    Expand-Archive -Path "newrelic.zip" -DestinationPath "." -Force
    Remove-Item "newrelic.zip"
    Write-Host "New Relic Java Agent descargado exitosamente"
}

# Ejecutar la aplicación con el agente de New Relic
Write-Host "Iniciando aplicación con New Relic Agent..."
.\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-javaagent:newrelic/newrelic.jar"
