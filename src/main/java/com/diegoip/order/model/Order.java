package com.diegoip.order.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    private String id;
    
    private String numeroPedido;
    
    private String dni;
    
    private List<OrderItem> items;
    
    private LocalDateTime fecha;
    
    private String estado;
    
    private Double total;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String sku;
        private String productoNombre;
        private Integer cantidad;
        private Double precioUnitario;
    }
}
