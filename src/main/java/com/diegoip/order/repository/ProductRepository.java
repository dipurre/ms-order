package com.diegoip.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.diegoip.order.model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    Optional<Product> findBySku(String sku);
    
    List<Product> findByCategoria(String categoria);
}
