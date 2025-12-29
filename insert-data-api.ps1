# Script PowerShell para insertar datos usando la API REST
# Asegúrate de que la aplicación esté ejecutándose en http://localhost:8080

$baseUrl = "http://localhost:8080/api"

Write-Host "🚀 Iniciando inserción de datos..." -ForegroundColor Green
Write-Host ""

# ============================================
# INSERTAR PRODUCTOS
# ============================================

Write-Host "📦 Insertando productos..." -ForegroundColor Cyan

$productos = @(
    @{
        sku = "LAPTOP-HP-001"
        stock = 15
        nombre = "Laptop HP Pavilion 15"
        categoria = "Computadoras"
    },
    @{
        sku = "MOUSE-LOG-002"
        stock = 50
        nombre = "Mouse Logitech MX Master 3"
        categoria = "Accesorios"
    },
    @{
        sku = "TECLADO-COR-003"
        stock = 30
        nombre = "Teclado Mecánico Corsair K70"
        categoria = "Accesorios"
    },
    @{
        sku = "MONITOR-DELL-004"
        stock = 20
        nombre = "Monitor Dell 27 pulgadas 4K"
        categoria = "Monitores"
    },
    @{
        sku = "HEADSET-HYP-005"
        stock = 40
        nombre = "Audífonos HyperX Cloud II"
        categoria = "Audio"
    },
    @{
        sku = "WEBCAM-LOG-006"
        stock = 25
        nombre = "Webcam Logitech C920"
        categoria = "Accesorios"
    },
    @{
        sku = "SSD-SAMS-007"
        stock = 60
        nombre = "SSD Samsung 1TB NVMe"
        categoria = "Almacenamiento"
    },
    @{
        sku = "RAM-CORS-008"
        stock = 35
        nombre = "Memoria RAM Corsair 16GB DDR4"
        categoria = "Componentes"
    },
    @{
        sku = "GPU-NVID-009"
        stock = 10
        nombre = "Tarjeta Gráfica NVIDIA RTX 3070"
        categoria = "Componentes"
    },
    @{
        sku = "CHAIR-SECR-010"
        stock = 18
        nombre = "Silla Gamer Secretlab"
        categoria = "Muebles"
    }
)

$productosInsertados = @{}

foreach ($producto in $productos) {
    try {
        $json = $producto | ConvertTo-Json
        $response = Invoke-RestMethod -Uri "$baseUrl/products" -Method Post -Body $json -ContentType "application/json"
        $productosInsertados[$producto.sku] = $response.id
        Write-Host "  ✅ Producto insertado: $($producto.nombre) - ID: $($response.id)" -ForegroundColor Green
    }
    catch {
        Write-Host "  ❌ Error insertando producto: $($producto.nombre)" -ForegroundColor Red
        Write-Host "     Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "✅ Productos insertados: $($productosInsertados.Count) de $($productos.Count)" -ForegroundColor Green
Write-Host ""

# Esperar un momento para asegurar que los datos estén disponibles
Start-Sleep -Seconds 2

# ============================================
# INSERTAR ÓRDENES
# ============================================

Write-Host "📋 Insertando órdenes..." -ForegroundColor Cyan

$ordenes = @(
    @{
        numeroPedido = "ORD-2024-001"
        dni = "12345678"
        items = @(
            @{
                productoId = $productosInsertados["LAPTOP-HP-001"]
                productoNombre = "Laptop HP Pavilion 15"
                cantidad = 1
                precioUnitario = 899.99
            },
            @{
                productoId = $productosInsertados["MOUSE-LOG-002"]
                productoNombre = "Mouse Logitech MX Master 3"
                cantidad = 1
                precioUnitario = 99.99
            }
        )
        fecha = "2024-12-20T10:30:00"
        estado = "ENTREGADO"
        total = 999.98
    },
    @{
        numeroPedido = "ORD-2024-002"
        dni = "87654321"
        items = @(
            @{
                productoId = $productosInsertados["TECLADO-COR-003"]
                productoNombre = "Teclado Mecánico Corsair K70"
                cantidad = 2
                precioUnitario = 149.99
            },
            @{
                productoId = $productosInsertados["HEADSET-HYP-005"]
                productoNombre = "Audífonos HyperX Cloud II"
                cantidad = 1
                precioUnitario = 79.99
            }
        )
        fecha = "2024-12-21T14:15:00"
        estado = "EN_PROCESO"
        total = 379.97
    },
    @{
        numeroPedido = "ORD-2024-003"
        dni = "45678912"
        items = @(
            @{
                productoId = $productosInsertados["MONITOR-DELL-004"]
                productoNombre = "Monitor Dell 27 pulgadas 4K"
                cantidad = 1
                precioUnitario = 499.99
            }
        )
        fecha = "2024-12-22T09:45:00"
        estado = "PENDIENTE"
        total = 499.99
    },
    @{
        numeroPedido = "ORD-2024-004"
        dni = "78912345"
        items = @(
            @{
                productoId = $productosInsertados["SSD-SAMS-007"]
                productoNombre = "SSD Samsung 1TB NVMe"
                cantidad = 2
                precioUnitario = 129.99
            },
            @{
                productoId = $productosInsertados["RAM-CORS-008"]
                productoNombre = "Memoria RAM Corsair 16GB DDR4"
                cantidad = 2
                precioUnitario = 89.99
            }
        )
        fecha = "2024-12-23T16:20:00"
        estado = "EN_PROCESO"
        total = 439.96
    },
    @{
        numeroPedido = "ORD-2024-005"
        dni = "32165498"
        items = @(
            @{
                productoId = $productosInsertados["GPU-NVID-009"]
                productoNombre = "Tarjeta Gráfica NVIDIA RTX 3070"
                cantidad = 1
                precioUnitario = 599.99
            },
            @{
                productoId = $productosInsertados["LAPTOP-HP-001"]
                productoNombre = "Laptop HP Pavilion 15"
                cantidad = 1
                precioUnitario = 899.99
            }
        )
        fecha = "2024-12-24T11:00:00"
        estado = "ENTREGADO"
        total = 1499.98
    },
    @{
        numeroPedido = "ORD-2024-006"
        dni = "65498732"
        items = @(
            @{
                productoId = $productosInsertados["CHAIR-SECR-010"]
                productoNombre = "Silla Gamer Secretlab"
                cantidad = 1
                precioUnitario = 449.99
            },
            @{
                productoId = $productosInsertados["WEBCAM-LOG-006"]
                productoNombre = "Webcam Logitech C920"
                cantidad = 1
                precioUnitario = 79.99
            }
        )
        fecha = "2024-12-25T13:30:00"
        estado = "PENDIENTE"
        total = 529.98
    },
    @{
        numeroPedido = "ORD-2024-007"
        dni = "98765432"
        items = @(
            @{
                productoId = $productosInsertados["MOUSE-LOG-002"]
                productoNombre = "Mouse Logitech MX Master 3"
                cantidad = 3
                precioUnitario = 99.99
            },
            @{
                productoId = $productosInsertados["TECLADO-COR-003"]
                productoNombre = "Teclado Mecánico Corsair K70"
                cantidad = 3
                precioUnitario = 149.99
            }
        )
        fecha = "2024-12-26T08:00:00"
        estado = "EN_PROCESO"
        total = 749.94
    },
    @{
        numeroPedido = "ORD-2024-008"
        dni = "11223344"
        items = @(
            @{
                productoId = $productosInsertados["HEADSET-HYP-005"]
                productoNombre = "Audífonos HyperX Cloud II"
                cantidad = 2
                precioUnitario = 79.99
            }
        )
        fecha = "2024-12-27T15:45:00"
        estado = "CANCELADO"
        total = 159.98
    }
)

$ordenesInsertadas = 0

foreach ($orden in $ordenes) {
    try {
        $json = $orden | ConvertTo-Json -Depth 10
        $response = Invoke-RestMethod -Uri "$baseUrl/orders" -Method Post -Body $json -ContentType "application/json"
        $ordenesInsertadas++
        Write-Host "  ✅ Orden insertada: $($orden.numeroPedido) - Total: $$$($orden.total)" -ForegroundColor Green
    }
    catch {
        Write-Host "  ❌ Error insertando orden: $($orden.numeroPedido)" -ForegroundColor Red
        Write-Host "     Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "✅ Órdenes insertadas: $ordenesInsertadas de $($ordenes.Count)" -ForegroundColor Green
Write-Host ""

# ============================================
# RESUMEN
# ============================================

Write-Host "📊 RESUMEN FINAL" -ForegroundColor Yellow
Write-Host "===============" -ForegroundColor Yellow
Write-Host "Productos insertados: $($productosInsertados.Count)" -ForegroundColor Cyan
Write-Host "Órdenes insertadas: $ordenesInsertadas" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Proceso completado!" -ForegroundColor Green

