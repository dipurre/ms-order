package com.diegoip.order.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.diegoip.order.model.Product;
import com.diegoip.order.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public List<Product> getAllProducts() {
        log.info("Obteniendo todos los productos");
        List<Product> products = productRepository.findAll();
        log.info("Se encontraron {} productos", products.size());
        return products;
    }
    
    public Optional<Product> getProductById(String id) {
        log.info("Buscando producto por ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            log.info("Producto encontrado: {}", product.get().getNombre());
        } else {
            log.warn("Producto no encontrado con ID: {}", id);
        }
        return product;
    }
    
    public Optional<Product> getProductBySku(String sku) {
        log.info("Buscando producto por SKU: {}", sku);
        Optional<Product> product = productRepository.findBySku(sku);
        if (product.isPresent()) {
            log.info("Producto encontrado: {}", product.get().getNombre());
        } else {
            log.warn("Producto no encontrado con SKU: {}", sku);
        }
        return product;
    }
    
    public List<Product> getProductsByCategoria(String categoria) {
        log.info("Buscando productos por categoría: {}", categoria);
        List<Product> products = productRepository.findByCategoria(categoria);
        log.info("Se encontraron {} productos en la categoría {}", products.size(), categoria);
        return products;
    }
    
    public Product createProduct(Product product) {
        log.info("Creando nuevo producto: {}", product.getNombre());

        // Validar que el SKU no exista
        if (product.getSku() != null && !product.getSku().isEmpty()) {
            Optional<Product> existingProduct = productRepository.findBySku(product.getSku());
            if (existingProduct.isPresent()) {
                log.error("Error: Ya existe un producto con el SKU: {}", product.getSku());
                throw new IllegalArgumentException("Ya existe un producto con el SKU: " + product.getSku());
            }
        } else {
            log.error("Error: El SKU es obligatorio");
            throw new IllegalArgumentException("El SKU es obligatorio");
        }

        Product savedProduct = productRepository.save(product);
        log.info("Producto creado exitosamente con ID: {} y SKU: {}", savedProduct.getId(), savedProduct.getSku());
        return savedProduct;
    }
    
    public Optional<Product> updateProduct(String id, Product productDetails) {
        log.info("Actualizando producto con ID: {}", id);

        // Validar que el SKU no esté siendo usado por otro producto
        if (productDetails.getSku() != null && !productDetails.getSku().isEmpty()) {
            Optional<Product> existingProductWithSku = productRepository.findBySku(productDetails.getSku());
            if (existingProductWithSku.isPresent() && !existingProductWithSku.get().getId().equals(id)) {
                log.error("Error: Ya existe otro producto con el SKU: {}", productDetails.getSku());
                throw new IllegalArgumentException("Ya existe otro producto con el SKU: " + productDetails.getSku());
            }
        }

        Optional<Product> updated = productRepository.findById(id)
                .map(product -> {
                    product.setSku(productDetails.getSku());
                    product.setStock(productDetails.getStock());
                    product.setNombre(productDetails.getNombre());
                    product.setCategoria(productDetails.getCategoria());
                    Product savedProduct = productRepository.save(product);
                    log.info("Producto actualizado exitosamente: {} con SKU: {}", savedProduct.getNombre(), savedProduct.getSku());
                    return savedProduct;
                });
        if (updated.isEmpty()) {
            log.warn("No se pudo actualizar, producto no encontrado con ID: {}", id);
        }
        return updated;
    }
    
    public boolean deleteProduct(String id) {
        log.info("Intentando eliminar producto con ID: {}", id);
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            log.info("Producto eliminado exitosamente con ID: {}", id);
            return true;
        }
        log.warn("No se pudo eliminar, producto no encontrado con ID: {}", id);
        return false;
    }
}
