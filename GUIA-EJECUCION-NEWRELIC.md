# Guía de Ejecución con New Relic

## Requisitos Previos

- Java 21 o superior
- Maven (o usar el wrapper incluido `mvnw.cmd`)
- MongoDB ejecutándose en `localhost:27017`
- Licencia de New Relic (opcional, si no está en `newrelic.yml`)

## Compilación

```powershell
# Compilar el proyecto
.\mvnw.cmd clean package

# Omitir tests si es necesario
.\mvnw.cmd clean package -DskipTests
```

## Ejecución con New Relic

### Opción 1: Sin especificar la licencia (usa newrelic.yml)

```powershell
.\run-newrelic-dev.ps1
```

### Opción 2: Especificando la licencia como parámetro

```powershell
.\run-newrelic-dev-with-license.ps1 -LicenseKey "TU_LICENSE_KEY_AQUI"
```

### Opción 3: Usando variable de entorno

```powershell
# Establecer la variable de entorno
$env:NEW_RELIC_LICENSE_KEY = "TU_LICENSE_KEY_AQUI"

# Ejecutar
.\run-newrelic-dev-with-license.ps1
```

### Opción 4: Línea de comandos directa

```powershell
java -javaagent:newrelic\newrelic-agent-8.25.1.jar `
     -Dspring.profiles.active=dev `
     -Dnewrelic.environment=dev `
     -Dnewrelic.config.app_name="ms-order[dev]" `
     -Dnewrelic.config.license_key="TU_LICENSE_KEY" `
     -jar target\ms-order-1.0.0-SNAPSHOT.jar
```

## Perfiles Disponibles

- **dev**: Desarrollo (configuración por defecto en los scripts)
- **prod**: Producción
- **test**: Testing

Para usar otro perfil, modifica `-Dspring.profiles.active=PERFIL` en el comando.

## Verificación

### 1. Verificar que la aplicación está corriendo

```powershell
curl http://localhost:8080/actuator/health
```

### 2. Verificar logs en consola

Los logs deberían mostrar:
```
New Relic Agent v8.25.1 is connected to collector.newrelic.com:443
```

### 3. Verificar en New Relic

1. Ir a: https://rpm.newrelic.com
2. Buscar la aplicación: `ms-order[dev]`
3. Los logs deberían aparecer en la sección "Logs"

### 4. Verificar ofuscación

Crear una orden con datos sensibles:

```powershell
$body = @{
    numeroPedido = "ORD-TEST-001"
    dni = "12345678"
    items = @(
        @{
            sku = "LAPTOP-HP-001"
            productoNombre = "Laptop HP"
            cantidad = 1
            precioUnitario = 899.99
        }
    )
    estado = "PENDIENTE"
    total = 899.99
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/orders" `
                  -Method Post `
                  -Body $body `
                  -ContentType "application/json"
```

**Verificar que:**
- En consola el DNI aparece ofuscado: `12***78`
- En New Relic el DNI también aparece ofuscado: `12***78`

## Patrones de Ofuscación

| Dato | Patrón | Ejemplo Original | Ejemplo Ofuscado |
|------|--------|------------------|------------------|
| DNI | 8 dígitos | `12345678` | `12***78` |
| Email | usuario@dominio.com | `usuario@example.com` | `u***@example.com` |
| Teléfono | 9-10 dígitos | `987654321` | `***321` |
| Tarjeta | 16 dígitos | `1234567890123456` | `***3456` |

## Configuración de New Relic

### Archivo newrelic.yml

La configuración está en el archivo raíz `newrelic.yml`:

```yaml
common: &default_settings
  license_key: '<%= license_key %>'
  app_name: ms-order

development:
  <<: *default_settings
  app_name: ms-order[dev]

production:
  <<: *default_settings
  app_name: ms-order[prod]
```

### Propiedades de Sistema

Puedes sobrescribir la configuración con propiedades de sistema:

| Propiedad | Descripción | Ejemplo |
|-----------|-------------|---------|
| `newrelic.config.license_key` | Licencia de New Relic | `-Dnewrelic.config.license_key=abc123...` |
| `newrelic.config.app_name` | Nombre de la app | `-Dnewrelic.config.app_name="ms-order[dev]"` |
| `newrelic.environment` | Ambiente | `-Dnewrelic.environment=dev` |

## Troubleshooting

### Error: "No se encontró el JAR"

**Solución:** Compila primero el proyecto:
```powershell
.\mvnw.cmd clean package
```

### Error: "release version 21 not supported"

**Solución:** Instala Java 21 o superior:
- Descargar desde: https://adoptium.net/
- Configurar JAVA_HOME

### Error: "No se encontró el agente de New Relic"

**Solución:** Verifica que existe el archivo:
```
newrelic\newrelic-agent-8.25.1.jar
```

### Los logs no aparecen en New Relic

**Posibles causas:**
1. Licencia incorrecta o no configurada
2. Firewall bloqueando conexión a `collector.newrelic.com:443`
3. El agente no está conectado (verificar logs de inicio)

**Verificación:**
```powershell
# Buscar en los logs
Select-String -Path "newrelic\logs\newrelic_agent.log" -Pattern "connected"
```

### Los datos NO están ofuscados en New Relic

**Verificación:**
1. Asegúrate de que `MaskingConsoleAppender` está compilado
2. Verifica que el `logback-spring.xml` usa el appender correcto
3. Reinicia la aplicación

## Endpoints Disponibles

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/products` | GET | Listar productos |
| `/api/products` | POST | Crear producto |
| `/api/products/{sku}` | GET | Obtener producto por SKU |
| `/api/orders` | GET | Listar órdenes |
| `/api/orders` | POST | Crear orden |
| `/api/orders/{numeroPedido}` | GET | Obtener orden |
| `/actuator/health` | GET | Estado de salud |
| `/actuator/info` | GET | Información de la app |

## Insertar Datos de Prueba

```powershell
# Ejecutar script de MongoDB
mongosh --file mongo-insert-data.js
```

O usando el script PowerShell:
```powershell
.\insert-data-api.ps1
```

## Logs

### Ubicación

- **Aplicación:** Consola + New Relic
- **New Relic Agent:** `newrelic\logs\newrelic_agent.log`

### Niveles

- `com.diegoip.order`: DEBUG
- `org.springframework`: INFO
- `org.springframework.data.mongodb`: INFO

Para cambiar el nivel, edita `src/main/resources/logback-spring.xml`.

## Documentación Adicional

- [Solución de Ofuscación](SOLUCION-OFUSCACION-NEWRELIC.md)
- [Scripts de MongoDB](MONGODB_SCRIPTS.md)
- [Validación SKU Único](VALIDACION-SKU-UNICO.md)
- [Datos de Prueba](README-DATOS.md)

## Soporte

Para problemas con:
- **New Relic:** https://docs.newrelic.com/
- **Spring Boot:** https://spring.io/projects/spring-boot
- **MongoDB:** https://docs.mongodb.com/

