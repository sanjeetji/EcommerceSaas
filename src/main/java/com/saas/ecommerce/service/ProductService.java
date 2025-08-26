package com.saas.ecommerce.service;

import com.saas.ecommerce.model.dto.ProductDto;
import com.saas.ecommerce.model.entity.Product;
import com.saas.ecommerce.repository.ProductRepository;
import com.saas.ecommerce.utils.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    public Product addProduct(ProductDto dto) {
        Product product = new Product();
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setDescription(dto.description());
        product.setClientId(TenantContext.getCurrentTenant());
        return repository.save(product);
    }

    public void updateProduct(Long id, ProductDto dto) {
        Product product = repository.findById(id).orElseThrow();
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setDescription(dto.description());
        repository.save(product);
    }

    public void deleteProduct(Long id) {
        repository.deleteById(id);
    }

    @Async
    public List<Product> listProducts() {
        return repository.findAll();
    }

    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }
}
