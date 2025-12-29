package com.diegoip.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.diegoip.order.model.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    
    List<Order> findByDni(String dni);
    
    Optional<Order> findByNumeroPedido(String numeroPedido);
}
