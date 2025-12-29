# Solución de Ofuscación para New Relic

## Problema
Los logs enviados a New Relic no estaban siendo ofuscados, exponiendo datos sensibles como DNI, emails, teléfonos y tarjetas de crédito.

## Solución Implementada

### 1. Clase MaskingNewRelicEncoder ⭐
Se creó un encoder personalizado que extiende `EncoderBase` y envuelve el `NewRelicEncoder` de New Relic, ofuscando los datos sensibles ANTES de codificar los mensajes.

**Ubicación:** `src/main/java/com/diegoip/order/config/MaskingNewRelicEncoder.java`

**Funcionalidad:**
- Envuelve el `NewRelicEncoder` oficial de New Relic
- Intercepta cada evento de logging antes de codificarlo
- Crea un nuevo evento con el mensaje ofuscado
- Pasa el evento ofuscado al encoder de New Relic
- Aplica patrones de ofuscación para:
  - **DNI (8 dígitos):** `12345678` → `12***78`
  - **Email:** `usuario@dominio.com` → `u***@dominio.com`
  - **Teléfono (9-10 dígitos):** `987654321` → `***321`
  - **Tarjeta (16 dígitos):** `1234567890123456` → `***3456`

### 2. Configuración de Logback
Se actualizó `logback-spring.xml` para usar el encoder personalizado:

```xml
<!-- New Relic Appender con Masking -->
<appender name="NEW_RELIC_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="com.diegoip.order.config.MaskingNewRelicEncoder"/>
</appender>

<appender name="NEW_RELIC_ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="NEW_RELIC_CONSOLE"/>
    <queueSize>512</queueSize>
</appender>
```

## Cómo Funciona

```
┌─────────────────────┐
│  Log generado       │
│  DNI: 12345678      │
└──────────┬──────────┘
           │
           v
┌───────────────────────────┐
│  ConsoleAppender          │
│  (ch.qos.logback.core)    │
└──────────┬────────────────┘
           │
           v
┌───────────────────────────┐
│  MaskingNewRelicEncoder   │  ⭐ AQUÍ SE OFUSCA
│  - Intercepta evento      │
│  - Ofusca mensaje         │
│  - Crea nuevo evento      │
│  - DNI: 12***78           │
└──────────┬────────────────┘
           │
           v
┌───────────────────────────┐
│  NewRelicEncoder          │
│  - Codifica en JSON       │
│  - Evento ya ofuscado     │
└──────────┬────────────────┘
           │
           v
┌───────────────────────────┐
│  New Relic APM            │
│  ✅ DNI: 12***78          │
└───────────────────────────┘
```

## Ventajas

- ✅ Los datos sensibles nunca salen de la aplicación sin ofuscar
- ✅ La ofuscación se aplica ANTES de que el encoder de New Relic procese el mensaje
- ✅ Funciona con el `AsyncAppender` para mejor rendimiento
- ✅ Mantiene la misma lógica de ofuscación en consola y New Relic
- ✅ Compatible con todas las características de New Relic (MDC, contexto, etc.)
- ✅ No afecta el rendimiento significativamente
- ✅ Fácil de mantener y extender

## Orden de Ofuscación

El orden es importante para evitar conflictos entre patrones:

1. **Tarjetas (16 dígitos)** - Se ofuscan primero
2. **Teléfonos (9-10 dígitos)** - Segundo
3. **DNI (8 dígitos)** - Tercero
4. **Emails** - Último

Este orden evita que un DNI dentro de una tarjeta sea tratado como DNI separado.

## Pruebas

Para verificar que funciona:

1. Ejecutar la aplicación con New Relic:
   ```powershell
   .\run-newrelic-dev-with-license.ps1 -LicenseKey "TU_KEY"
   ```

2. Hacer peticiones con datos sensibles:
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

3. Verificar en New Relic que el DNI aparece ofuscado: `12***78`

## Compilación

Para compilar el proyecto (requiere Java 21):
```powershell
.\mvnw.cmd clean package
```

Para ejecutar con New Relic:
```powershell
.\run-newrelic-dev-with-license.ps1 -LicenseKey "TU_LICENSE_KEY"
```

## Archivos Creados

1. **MaskingNewRelicEncoder.java** ⭐ - Encoder principal (SOLUCIÓN FINAL)
2. **MaskingConsoleAppender.java** - Appender alternativo (no usado)
3. **MaskingNewRelicAppender.java** - Appender alternativo (no usado)
4. **MaskingMessageConverter.java** - Converter alternativo (no usado)
5. **MaskingFilter.java** - Filter alternativo (no usado)
6. **MaskingTurboFilter.java** - TurboFilter alternativo (no usado)

## Mantenimiento Futuro

Para agregar nuevos patrones de ofuscación:
1. Editar `MaskingNewRelicEncoder.java`
2. Agregar el nuevo patrón como constante estática
3. Agregar el método de ofuscación privado
4. Agregar la llamada en el método `maskMessage()`

Ejemplo para agregar ofuscación de RUC (11 dígitos):
```java
// En las constantes
private static final Pattern RUC_PATTERN = Pattern.compile("\\b\\d{11}\\b");

// En maskMessage(), después de ofuscar teléfonos y antes de DNI
masked = maskPattern(masked, RUC_PATTERN, this::maskRuc);

// Método de ofuscación
private String maskRuc(String ruc) {
    if (ruc == null || ruc.length() != 11) {
        return "***";
    }
    return ruc.substring(0, 3) + "***" + ruc.substring(8);
}
```

## Notas Importantes

- El encoder se inicializa en el método `start()` con el contexto de Logback
- El `NewRelicEncoder` envuelto recibe el contexto completo de la aplicación
- El `AsyncAppender` mejora el rendimiento procesando logs en segundo plano
- Los eventos ofuscados mantienen toda la información de contexto (MDC, timestamp, level, etc.)

## Troubleshooting

### Los logs no están ofuscados en New Relic

**Verificación:**
1. Asegúrate de que `MaskingNewRelicEncoder` está compilado correctamente
2. Verifica que el `logback-spring.xml` usa el encoder correcto:
   ```xml
   <encoder class="com.diegoip.order.config.MaskingNewRelicEncoder"/>
   ```
3. Reinicia la aplicación completamente
4. Verifica en los logs de inicio que Logback está usando la configuración correcta

### Error al iniciar: "Cannot instantiate MaskingNewRelicEncoder"

**Causa:** La clase no está compilada o no está en el classpath

**Solución:**
```powershell
.\mvnw.cmd clean compile
```

### Los logs no llegan a New Relic

**Verificación:**
1. Verifica la licencia de New Relic
2. Revisa los logs del agente: `newrelic\logs\newrelic_agent.log`
3. Verifica que el agente esté conectado:
   ```powershell
   Select-String -Path "newrelic\logs\newrelic_agent.log" -Pattern "connected"
   ```

---

**¡Solución completa implementada y lista para usar! 🔒**

