package com.diegoip.order.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.diegoip.order.model.Order;
import com.diegoip.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    public List<Order> getAllOrders() {
        log.info("Obteniendo todos los pedidos");
        List<Order> orders = orderRepository.findAll();
        log.info("Se encontraron {} pedidos", orders.size());
        return orders;
    }
    
    public Optional<Order> getOrderById(String id) {
        log.info("Buscando pedido por ID: {}", id);
        Optional<Order> order = orderRepository.findById(id);
        if (order.isPresent()) {
            log.info("Pedido encontrado: {}", order.get().getNumeroPedido());
        } else {
            log.warn("Pedido no encontrado con ID: {}", id);
        }
        return order;
    }
    
    public List<Order> getOrdersByDni(String dni) {
        log.info("Buscando pedidos por DNI: {}", dni);
        List<Order> orders = orderRepository.findByDni(dni);
        log.info("Se encontraron {} pedidos para el DNI: {}", orders.size(), dni);
        return orders;
    }
    
    public Optional<Order> getOrderByNumeroPedido(String numeroPedido) {
        log.info("Buscando pedido por número: {}", numeroPedido);
        Optional<Order> order = orderRepository.findByNumeroPedido(numeroPedido);
        if (order.isPresent()) {
            log.info("Pedido encontrado para número: {}", numeroPedido);
        } else {
            log.warn("Pedido no encontrado con número: {}", numeroPedido);
        }
        return order;
    }
    
    public Order createOrder(Order order) {
        log.info("Creando nuevo pedido para DNI: {}", order.getDni());
        Order savedOrder = orderRepository.save(order);
        log.info("Pedido creado exitosamente con número: {}", savedOrder.getNumeroPedido());
        return savedOrder;
    }
    
    public Optional<Order> updateOrder(String id, Order orderDetails) {
        log.info("Actualizando pedido con ID: {}", id);
        Optional<Order> updated = orderRepository.findById(id)
                .map(order -> {
                    order.setNumeroPedido(orderDetails.getNumeroPedido());
                    order.setDni(orderDetails.getDni());
                    order.setItems(orderDetails.getItems());
                    order.setFecha(orderDetails.getFecha());
                    order.setEstado(orderDetails.getEstado());
                    order.setTotal(orderDetails.getTotal());
                    Order savedOrder = orderRepository.save(order);
                    log.info("Pedido actualizado exitosamente: {}", savedOrder.getNumeroPedido());
                    return savedOrder;
                });
        if (updated.isEmpty()) {
            log.warn("No se pudo actualizar, pedido no encontrado con ID: {}", id);
        }
        return updated;
    }
    
    public boolean deleteOrder(String id) {
        log.info("Intentando eliminar pedido con ID: {}", id);
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            log.info("Pedido eliminado exitosamente con ID: {}", id);
            return true;
        }
        log.warn("No se pudo eliminar, pedido no encontrado con ID: {}", id);
        return false;
    }
}
