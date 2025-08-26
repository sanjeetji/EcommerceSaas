package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.User;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.jpa.repository.JpaRepository;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "currentTenant", type = Long.class))
@Filter(name = "tenantFilter", condition = "client_id = :currentTenant")
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
