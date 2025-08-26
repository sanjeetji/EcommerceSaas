package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.Product;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.jpa.repository.JpaRepository;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "currentTenant", type = Long.class))
@Filter(name = "tenantFilter", condition = "client_id = :currentTenant")
public interface ProductRepository extends JpaRepository<Product, Long> {}
