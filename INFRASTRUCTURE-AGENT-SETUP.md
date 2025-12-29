# 📦 Configuración del Infrastructure Agent para Log Forwarding

## 🎯 Objetivo

Configurar el New Relic Infrastructure Agent para leer logs **ofuscados** desde archivos y enviarlos a New Relic con el formato completo (incluyendo `trace.id`, `span.id`, etc.).

## ✅ Ventajas de esta Opción

| Característica | Descripción |
|----------------|-------------|
| ✅ Logs ofuscados | Los logs se escriben ya ofuscados en el archivo |
| ✅ Formato completo | Incluye todos los campos de New Relic |
| ✅ Desacoplado | No depende del Java Agent para logs |
| ✅ Persistencia | Los logs quedan en disco como backup |
| ✅ Escalable | Funciona con múltiples instancias |

## 📋 Pasos de Instalación

### 1. Instalar el Infrastructure Agent

#### Windows (PowerShell como Administrador):
```powershell
# Descargar e instalar
$LICENSE_KEY = "TU_LICENSE_KEY"

# Método 1: MSI Installer
Invoke-WebRequest -Uri "https://download.newrelic.com/infrastructure_agent/windows/newrelic-infra.msi" -OutFile "newrelic-infra.msi"
msiexec.exe /qn /i newrelic-infra.msi GENERATE_CONFIG=true LICENSE_KEY="$LICENSE_KEY"

# Método 2: Chocolatey
choco install newrelic-infra -y
```

#### Linux (Ubuntu/Debian):
```bash
# Agregar repositorio
curl -s https://download.newrelic.com/infrastructure_agent/gpg/newrelic-infra.gpg | sudo apt-key add -
echo "deb https://download.newrelic.com/infrastructure_agent/linux/apt focal main" | sudo tee /etc/apt/sources.list.d/newrelic-infra.list

# Instalar
sudo apt-get update
sudo apt-get install newrelic-infra -y
```

#### Linux (RHEL/CentOS):
```bash
# Agregar repositorio
sudo curl -o /etc/yum.repos.d/newrelic-infra.repo https://download.newrelic.com/infrastructure_agent/linux/yum/el/8/x86_64/newrelic-infra.repo

# Instalar
sudo yum install newrelic-infra -y
```

### 2. Configurar el Infrastructure Agent

#### Windows:
Editar `C:\Program Files\New Relic\newrelic-infra\newrelic-infra.yml`:

```yaml
license_key: TU_LICENSE_KEY
display_name: ms-order-server
log:
  level: info
  forward: true
```

#### Linux:
Editar `/etc/newrelic-infra.yml`:

```yaml
license_key: TU_LICENSE_KEY
display_name: ms-order-server
log:
  level: info
  forward: true
```

### 3. Configurar Log Forwarding

#### Windows:
Copiar el archivo de configuración:
```powershell
# Crear directorio si no existe
New-Item -Path "C:\Program Files\New Relic\newrelic-infra\logging.d" -ItemType Directory -Force

# Copiar configuración
Copy-Item "E:\proyectos_diegoip\ms-order\infrastructure-agent\logging.d\ms-order.yml" `
          "C:\Program Files\New Relic\newrelic-infra\logging.d\ms-order.yml"
```

#### Linux:
```bash
# Copiar configuración
sudo cp /path/to/ms-order/infrastructure-agent/logging.d/ms-order.yml /etc/newrelic-infra/logging.d/
```

### 4. Ajustar la configuración de logs

Editar el archivo copiado según tu entorno:

**Windows:** `C:\Program Files\New Relic\newrelic-infra\logging.d\ms-order.yml`
**Linux:** `/etc/newrelic-infra/logging.d/ms-order.yml`

```yaml
logs:
  - name: ms-order-json-logs
    # Ajustar la ruta al archivo de logs JSON
    file: E:\proyectos_diegoip\ms-order\logs\ms-order-json.log  # Windows
    # file: /var/log/ms-order/ms-order-json.log                 # Linux
    attributes:
      application: ms-order
      environment: dev
      team: backend
      logtype: application
```

### 5. Reiniciar el Infrastructure Agent

#### Windows:
```powershell
Restart-Service newrelic-infra
```

#### Linux:
```bash
sudo systemctl restart newrelic-infra
```

### 6. Verificar que el agente está funcionando

#### Windows:
```powershell
Get-Service newrelic-infra
Get-Content "C:\Program Files\New Relic\newrelic-infra\newrelic-infra.log" -Tail 50
```

#### Linux:
```bash
sudo systemctl status newrelic-infra
sudo tail -f /var/log/newrelic-infra/newrelic-infra.log
```

## 🔧 Configuración de la Aplicación

### Estructura de archivos de log creados:

```
E:\proyectos_diegoip\ms-order\
└── logs\
    ├── ms-order-json.log      ← Logs en formato JSON (para Infrastructure Agent)
    ├── ms-order.log           ← Logs en formato texto (backup)
    └── ms-order-json.*.log.gz ← Logs rotados comprimidos
```

### Formato del archivo JSON (`ms-order-json.log`):

```json
{"timestamp":1735500000000,"level":"INFO","logger.name":"com.diegoip.order.service.OrderService","message":"Buscando pedidos por DNI: 12***78","thread.name":"http-nio-8080-exec-1","trace.id":"abc123...","span.id":"def456...","entity.name":"ms-order[dev]"}
{"timestamp":1735500001000,"level":"DEBUG","logger.name":"com.diegoip.order.repository.OrderRepository","message":"Query executed successfully","thread.name":"http-nio-8080-exec-1"}
```

### Formato del archivo texto (`ms-order.log`):

```
2024-12-29 10:00:00.000 [http-nio-8080-exec-1] INFO  c.d.o.service.OrderService - Buscando pedidos por DNI: 12***78
2024-12-29 10:00:01.000 [http-nio-8080-exec-1] DEBUG c.d.o.repository.OrderRepository - Query executed successfully
```

## 📊 Verificar en New Relic

### 1. Ver logs en la UI:

1. Ir a **New Relic One > Logs**
2. Filtrar por: `application:ms-order`
3. Los logs deberían aparecer con el mensaje ofuscado

### 2. Query NRQL:

```sql
-- Ver logs de la aplicación
SELECT * FROM Log 
WHERE application = 'ms-order' 
SINCE 1 hour ago

-- Ver logs con datos ofuscados
SELECT message, level, logger.name, trace.id 
FROM Log 
WHERE application = 'ms-order' 
AND message LIKE '%DNI%'
SINCE 1 hour ago

-- Contar logs por nivel
SELECT count(*) FROM Log 
WHERE application = 'ms-order' 
FACET level 
SINCE 1 hour ago
```

## 🔄 Flujo de Datos

```
┌─────────────────────────────────────────────────────────────┐
│ Aplicación Spring Boot                                      │
│                                                             │
│ log.info("DNI: {}", "12345678")                            │
│           │                                                 │
│           v                                                 │
│ ┌─────────────────────────────────────────────────────┐    │
│ │ MaskingPatternLayoutV2 / MaskingNewRelicEncoder     │    │
│ │ Ofusca: "DNI: 12***78"                              │    │
│ └──────────────────┬──────────────────────────────────┘    │
│                    │                                        │
│           ┌────────┴────────┐                              │
│           v                 v                               │
│  ┌──────────────┐  ┌──────────────────┐                    │
│  │ FILE_TEXT    │  │ FILE_JSON        │                    │
│  │ ms-order.log │  │ ms-order-json.log│                    │
│  └──────────────┘  └────────┬─────────┘                    │
└─────────────────────────────│───────────────────────────────┘
                              │
                              v
┌─────────────────────────────────────────────────────────────┐
│ New Relic Infrastructure Agent                              │
│                                                             │
│ - Lee: logs/ms-order-json.log                              │
│ - Parsea JSON automáticamente                               │
│ - Agrega atributos: application, environment, team          │
│ - Envía a New Relic                                         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               v
┌─────────────────────────────────────────────────────────────┐
│ New Relic Logs                                              │
│                                                             │
│ {                                                           │
│   "message": "Buscando pedidos por DNI: 12***78",          │
│   "level": "INFO",                                          │
│   "logger.name": "c.d.o.service.OrderService",             │
│   "trace.id": "abc123...",                                  │
│   "span.id": "def456...",                                   │
│   "application": "ms-order",                                │
│   "environment": "dev"                                      │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ Troubleshooting

### Los logs no aparecen en New Relic

1. **Verificar que el archivo de log existe:**
   ```powershell
   Test-Path "E:\proyectos_diegoip\ms-order\logs\ms-order-json.log"
   Get-Content "E:\proyectos_diegoip\ms-order\logs\ms-order-json.log" -Tail 10
   ```

2. **Verificar permisos del Infrastructure Agent:**
   ```powershell
   # El servicio debe tener acceso de lectura al archivo
   icacls "E:\proyectos_diegoip\ms-order\logs"
   ```

3. **Verificar logs del Infrastructure Agent:**
   ```powershell
   Get-Content "C:\Program Files\New Relic\newrelic-infra\newrelic-infra.log" -Tail 100 | Select-String "logging"
   ```

4. **Verificar configuración:**
   ```powershell
   Get-Content "C:\Program Files\New Relic\newrelic-infra\logging.d\ms-order.yml"
   ```

### Error: "file not found"

- Asegurarse de que la aplicación Spring Boot esté corriendo y generando logs
- Verificar que la ruta en el archivo de configuración sea correcta
- En Windows, usar rutas absolutas con `\` o `/`

### Error: "permission denied"

- En Windows, ejecutar el Infrastructure Agent como Administrador
- En Linux, dar permisos de lectura al usuario `newrelic-infra`:
  ```bash
  sudo chmod 644 /var/log/ms-order/*.log
  sudo chown newrelic-infra:newrelic-infra /var/log/ms-order/*.log
  ```

## 📁 Archivos de Configuración

### 1. `logback-spring.xml` (ya configurado)
- `FILE_JSON`: Escribe logs ofuscados en formato JSON
- `FILE_TEXT`: Escribe logs ofuscados en formato texto

### 2. `infrastructure-agent/logging.d/ms-order.yml`
- Configuración del Infrastructure Agent para leer logs

### 3. `newrelic.yml` (opcional)
- `application_logging.forwarding.enabled: false` para evitar duplicados

## ⚠️ Notas Importantes

1. **No duplicar logs:** Si usas el Infrastructure Agent, desactiva `forwarding` en el Java Agent
2. **Rotación de logs:** Configurada para 50MB por archivo, 7 días de retención
3. **Permisos:** El Infrastructure Agent necesita acceso de lectura a los archivos de log
4. **Formato JSON:** El Infrastructure Agent parsea automáticamente logs JSON

## 🎉 Resultado Final

Con esta configuración:
- ✅ Los logs se escriben ofuscados en archivos
- ✅ El Infrastructure Agent los lee y envía a New Relic
- ✅ Los logs aparecen con todos los campos necesarios
- ✅ Los datos sensibles (DNI, email, etc.) están protegidos
- ✅ Tienes backup local de los logs

---

**¡Configuración completa del Infrastructure Agent para Log Forwarding!** 🚀

