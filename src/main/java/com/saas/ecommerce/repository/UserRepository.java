package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email")
    User findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.clientId = :clientId")
    Optional<List<User>> findByClientId(Long clientId);

}