package com.diegoip.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diegoip.order.model.Order;
import com.diegoip.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("GET /api/orders - Solicitando listado completo de pedidos");
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        log.info("GET /api/orders/{} - Solicitando pedido por ID", id);
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/dni/{dni}")
    public ResponseEntity<List<Order>> getOrdersByDni(@PathVariable String dni) {
        log.info("GET /api/orders/dni/{} - Solicitando pedidos por DNI", dni);
        List<Order> orders = orderService.getOrdersByDni(dni);
        if (orders.isEmpty()) {
            log.info("No se encontraron pedidos para el DNI: {}", dni);
        }
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/numero/{numeroPedido}")
    public ResponseEntity<Order> getOrderByNumeroPedido(@PathVariable String numeroPedido) {
        log.info("GET /api/orders/numero/{} - Solicitando pedido por número", numeroPedido);
        return orderService.getOrderByNumeroPedido(numeroPedido)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        log.info("POST /api/orders - Creando nuevo pedido para DNI: {}", order.getDni());
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable String id, @RequestBody Order order) {
        log.info("PUT /api/orders/{} - Actualizando pedido", id);
        return orderService.updateOrder(id, order)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        log.info("DELETE /api/orders/{} - Eliminando pedido", id);
        if (orderService.deleteOrder(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
