package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.ProductDto;
import com.saas.ecommerce.model.entity.Product;
import com.saas.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product add(@RequestBody ProductDto dto) {
        return service.addProduct(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@PathVariable Long id, @RequestBody ProductDto dto) {
        service.updateProduct(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.deleteProduct(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<Product> list() {
        return service.listProducts();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProduct(id));
    }
}
