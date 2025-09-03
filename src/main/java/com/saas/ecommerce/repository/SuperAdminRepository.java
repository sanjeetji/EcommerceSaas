package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
    @Query("SELECT sa FROM SuperAdmin sa WHERE sa.email = :email")
    Optional<SuperAdmin> findByEmail(String email);

    @Query("SELECT sa FROM SuperAdmin sa WHERE sa.id = :id")
    SuperAdmin findById();
}