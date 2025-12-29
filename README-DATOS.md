# 📝 Guía de Inserción de Datos - MS Product

Este directorio contiene varios scripts para insertar datos de prueba en tu aplicación de productos y órdenes.

## 📁 Archivos Disponibles

1. **`mongo-insert-data.js`** - Script JavaScript para MongoDB Shell
2. **`insert-data-api.ps1`** - Script PowerShell para insertar datos vía API REST
3. **`MONGODB_SCRIPTS.md`** - Documentación detallada con múltiples métodos

## 🚀 Métodos de Inserción

### Opción 1: Directamente en MongoDB (Recomendado para desarrollo)

#### Usando MongoDB Shell (mongosh):

```bash
# Conectar a tu base de datos
mongosh "mongodb://localhost:27017/productdb"

# Ejecutar el script
load('mongo-insert-data.js')
```

O desde PowerShell:
```powershell
mongosh "mongodb://localhost:27017/productdb" < mongo-insert-data.js
```

**Ventajas:**
- ✅ Más rápido
- ✅ No requiere que la aplicación esté ejecutándose
- ✅ Inserta datos con IDs específicos

---

### Opción 2: A través de la API REST (Recomendado para pruebas de integración)

#### Paso 1: Iniciar la aplicación
```powershell
cd E:\proyectos_diegoip\ms-product
.\mvnw.cmd spring-boot:run
```

#### Paso 2: Ejecutar el script PowerShell
En otra terminal:
```powershell
cd E:\proyectos_diegoip\ms-product
.\insert-data-api.ps1
```

**Ventajas:**
- ✅ Prueba los endpoints de la API
- ✅ Valida que los controladores funcionen correctamente
- ✅ Los IDs son generados automáticamente por MongoDB

---

### Opción 3: Manualmente con MongoDB Compass

1. Abre MongoDB Compass
2. Conecta a `mongodb://localhost:27017`
3. Selecciona la base de datos `productdb`
4. Ve a la colección `products` o `orders`
5. Haz clic en "ADD DATA" > "Insert Document"
6. Copia y pega los datos desde `MONGODB_SCRIPTS.md`

---

## 📊 Datos que se Insertarán

### 10 Productos:
- Laptop HP Pavilion 15
- Mouse Logitech MX Master 3
- Teclado Mecánico Corsair K70
- Monitor Dell 27 pulgadas 4K
- Audífonos HyperX Cloud II
- Webcam Logitech C920
- SSD Samsung 1TB NVMe
- Memoria RAM Corsair 16GB DDR4
- Tarjeta Gráfica NVIDIA RTX 3070
- Silla Gamer Secretlab

### 8 Órdenes:
- Con diferentes estados: `ENTREGADO`, `EN_PROCESO`, `PENDIENTE`, `CANCELADO`
- Con múltiples items por orden
- Con diferentes DNIs de clientes
- Con fechas variadas en diciembre 2024

---

## ✅ Verificar que los Datos se Insertaron

### Opción 1: Usando MongoDB Shell
```javascript
// Contar documentos
db.products.countDocuments()  // Debe retornar: 10
db.orders.countDocuments()     // Debe retornar: 8

// Ver todos los productos
db.products.find().pretty()

// Ver todas las órdenes
db.orders.find().pretty()
```

### Opción 2: Usando la API REST
```powershell
# Obtener todos los productos
Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Get

# Obtener todas las órdenes
Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Get

# Buscar orden por DNI
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/dni/12345678" -Method Get
```

### Opción 3: Usando curl
```bash
# Obtener todos los productos
curl http://localhost:8080/api/products

# Obtener todas las órdenes
curl http://localhost:8080/api/orders

# Buscar orden por DNI
curl http://localhost:8080/api/orders/dni/12345678
```

---

## 🧹 Limpiar los Datos

Si necesitas eliminar todos los datos y empezar de nuevo:

### Usando MongoDB Shell:
```javascript
// Conectar a la base de datos
use productdb

// Eliminar todos los productos
db.products.deleteMany({})

// Eliminar todas las órdenes
db.orders.deleteMany({})
```

### Usando PowerShell:
```powershell
# Script para limpiar datos
$baseUrl = "http://localhost:8080/api"

# Obtener y eliminar todos los productos
$productos = Invoke-RestMethod -Uri "$baseUrl/products" -Method Get
foreach ($producto in $productos) {
    Invoke-RestMethod -Uri "$baseUrl/products/$($producto.id)" -Method Delete
    Write-Host "Producto eliminado: $($producto.nombre)"
}

# Obtener y eliminar todas las órdenes
$ordenes = Invoke-RestMethod -Uri "$baseUrl/orders" -Method Get
foreach ($orden in $ordenes) {
    Invoke-RestMethod -Uri "$baseUrl/orders/$($orden.id)" -Method Delete
    Write-Host "Orden eliminada: $($orden.numeroPedido)"
}
```

---

## 🔍 Consultas de Ejemplo

### Buscar productos por categoría:
```javascript
db.products.find({ categoria: "Accesorios" })
```

### Buscar órdenes en proceso:
```javascript
db.orders.find({ estado: "EN_PROCESO" })
```

### Buscar órdenes de un cliente específico:
```javascript
db.orders.find({ dni: "12345678" })
```

### Calcular el total de ventas:
```javascript
db.orders.aggregate([
    { $group: { _id: null, totalVentas: { $sum: "$total" } } }
])
```

### Productos con stock bajo (menos de 20):
```javascript
db.products.find({ stock: { $lt: 20 } })
```

---

## 🐛 Troubleshooting

### Error: "mongosh no se reconoce como comando"
- Asegúrate de tener MongoDB Shell instalado
- Descarga desde: https://www.mongodb.com/try/download/shell

### Error: "No se puede conectar a MongoDB"
- Verifica que MongoDB esté ejecutándose: `mongod`
- Verifica la URL de conexión en `application.yaml`

### Error al ejecutar el script PowerShell
- Ejecuta: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`
- Luego intenta de nuevo

### La aplicación no inicia
- Verifica que MongoDB esté ejecutándose
- Verifica el puerto 8080 esté libre
- Revisa los logs en `logs/product-error.log`

---

## 📞 Contacto

Si tienes problemas o preguntas, revisa:
- Los logs de la aplicación: `logs/product-app.log`
- Los logs de errores: `logs/product-error.log`
- La documentación completa en `MONGODB_SCRIPTS.md`

