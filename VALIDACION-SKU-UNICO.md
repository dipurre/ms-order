# Validación de SKU Único en MongoDB

## ✅ Implementación Completada

Se ha implementado la validación de SKU único para los productos en MongoDB con las siguientes características:

### 1. **Índice Único en MongoDB**
- ✅ Campo `sku` marcado con `@Indexed(unique = true)` en el modelo `Product`
- ✅ Configuración `auto-index-creation: true` en `application.yaml`
- ✅ MongoDB creará automáticamente el índice único al iniciar la aplicación

### 2. **Validación en el Servicio**
- ✅ Validación antes de crear un producto (método `createProduct`)
- ✅ Validación antes de actualizar un producto (método `updateProduct`)
- ✅ Mensajes de error descriptivos en los logs

### 3. **Manejo de Excepciones**
- ✅ `IllegalArgumentException` cuando se intenta usar un SKU duplicado
- ✅ Respuesta HTTP 409 (Conflict) con mensaje de error
- ✅ Respuesta JSON con el mensaje de error

---

## 🧪 Pruebas de Validación

### Caso 1: Crear un producto con SKU único ✅
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-001",
    "nombre": "Producto de Prueba",
    "stock": 10,
    "categoria": "Pruebas"
  }'
```

**Respuesta esperada:** HTTP 201 Created
```json
{
  "id": "67...",
  "sku": "TEST-001",
  "nombre": "Producto de Prueba",
  "stock": 10,
  "categoria": "Pruebas"
}
```

### Caso 2: Intentar crear otro producto con el mismo SKU ❌
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-001",
    "nombre": "Otro Producto",
    "stock": 5,
    "categoria": "Pruebas"
  }'
```

**Respuesta esperada:** HTTP 409 Conflict
```json
{
  "message": "Ya existe un producto con el SKU: TEST-001"
}
```

### Caso 3: Crear producto sin SKU ❌
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Producto sin SKU",
    "stock": 10,
    "categoria": "Pruebas"
  }'
```

**Respuesta esperada:** HTTP 409 Conflict
```json
{
  "message": "El SKU es obligatorio"
}
```

### Caso 4: Actualizar producto cambiando a un SKU existente ❌
```bash
# Primero crear dos productos
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku": "TEST-002", "nombre": "Producto 2", "stock": 10, "categoria": "Pruebas"}'

curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku": "TEST-003", "nombre": "Producto 3", "stock": 10, "categoria": "Pruebas"}'

# Intentar actualizar TEST-003 para usar el SKU TEST-002
curl -X PUT http://localhost:8080/api/products/{id_de_TEST-003} \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-002",
    "nombre": "Producto 3 Modificado",
    "stock": 15,
    "categoria": "Pruebas"
  }'
```

**Respuesta esperada:** HTTP 409 Conflict
```json
{
  "message": "Ya existe otro producto con el SKU: TEST-002"
}
```

### Caso 5: Actualizar producto manteniendo su propio SKU ✅
```bash
curl -X PUT http://localhost:8080/api/products/{id_del_producto} \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-001",
    "nombre": "Producto de Prueba Actualizado",
    "stock": 20,
    "categoria": "Pruebas"
  }'
```

**Respuesta esperada:** HTTP 200 OK
```json
{
  "id": "67...",
  "sku": "TEST-001",
  "nombre": "Producto de Prueba Actualizado",
  "stock": 20,
  "categoria": "Pruebas"
}
```

---

## 🔍 Verificar el Índice en MongoDB

### Usando mongosh:
```javascript
// Conectar a la base de datos
use admin

// Ver los índices de la colección products
db.products.getIndexes()

// Debería mostrar algo como:
[
  { v: 2, key: { _id: 1 }, name: '_id_' },
  { v: 2, key: { sku: 1 }, name: 'sku', unique: true }
]
```

### Usando MongoDB Compass:
1. Conecta a tu base de datos
2. Ve a la colección `products`
3. Haz clic en la pestaña "Indexes"
4. Deberías ver el índice `sku` con la propiedad `unique: true`

---

## 📋 Script PowerShell para Pruebas Completas

```powershell
# Test de validación de SKU único
$baseUrl = "http://localhost:8080/api/products"

Write-Host "🧪 Iniciando pruebas de SKU único..." -ForegroundColor Cyan
Write-Host ""

# Caso 1: Crear producto con SKU único
Write-Host "📝 Caso 1: Crear producto con SKU único" -ForegroundColor Yellow
$producto1 = @{
    sku = "TEST-UNIQUE-001"
    nombre = "Producto Prueba 1"
    stock = 10
    categoria = "Pruebas"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $producto1 -ContentType "application/json"
    Write-Host "✅ ÉXITO: Producto creado con ID: $($response.id)" -ForegroundColor Green
    $productoId = $response.id
} catch {
    Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Caso 2: Intentar crear producto con SKU duplicado
Write-Host "📝 Caso 2: Intentar crear producto con SKU duplicado" -ForegroundColor Yellow
$producto2 = @{
    sku = "TEST-UNIQUE-001"
    nombre = "Producto Duplicado"
    stock = 5
    categoria = "Pruebas"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $producto2 -ContentType "application/json"
    Write-Host "❌ FALLO: Se permitió crear producto con SKU duplicado" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✅ ÉXITO: Se rechazó el SKU duplicado (HTTP 409)" -ForegroundColor Green
    } else {
        Write-Host "❌ ERROR INESPERADO: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# Limpiar datos de prueba
Write-Host "🧹 Limpiando datos de prueba..." -ForegroundColor Cyan
if ($productoId) {
    try {
        Invoke-RestMethod -Uri "$baseUrl/$productoId" -Method Delete
        Write-Host "✅ Producto de prueba eliminado" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ No se pudo eliminar el producto de prueba" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "✅ Pruebas completadas!" -ForegroundColor Green
```

---

## 🚨 Logs de la Aplicación

Cuando se intenta crear un producto con SKU duplicado, verás logs como:

```
2024-12-27 20:30:00.123 [http-nio-8080-exec-1] INFO  c.d.p.controller.ProductController - POST /api/products - Creando nuevo producto: Producto Duplicado
2024-12-27 20:30:00.124 [http-nio-8080-exec-1] INFO  c.d.p.service.ProductService - Creando nuevo producto: Producto Duplicado
2024-12-27 20:30:00.125 [http-nio-8080-exec-1] ERROR c.d.p.service.ProductService - Error: Ya existe un producto con el SKU: TEST-001
2024-12-27 20:30:00.126 [http-nio-8080-exec-1] ERROR c.d.p.controller.ProductController - Error al crear producto: Ya existe un producto con el SKU: TEST-001
```

---

## 📚 Archivos Modificados

1. **`Product.java`**
   - Agregado: `@Indexed(unique = true)` en el campo `sku`

2. **`ProductService.java`**
   - Agregada validación en `createProduct()`
   - Agregada validación en `updateProduct()`

3. **`ProductController.java`**
   - Agregado manejo de excepciones con try-catch
   - Agregada clase `ErrorResponse` para respuestas de error

4. **`application.yaml`**
   - Agregado: `auto-index-creation: true`

---

## ✅ Resultado Final

- ✅ El SKU es único por producto
- ✅ MongoDB rechazará duplicados a nivel de base de datos
- ✅ La aplicación valida antes de insertar/actualizar
- ✅ Respuestas HTTP apropiadas (409 Conflict)
- ✅ Mensajes de error descriptivos
- ✅ Logs detallados para debugging

¡La validación de SKU único está completamente implementada! 🎉

