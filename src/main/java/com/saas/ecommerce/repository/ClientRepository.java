package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query("SELECT c FROM Client c WHERE c.email = :email")
    Optional<Client> findByEmail(String email);

    @Query("SELECT c FROM Client c WHERE c.clientApiKey = :clientApiKey")
    Optional<Client> findByClientApiKey(String clientApiKey);

}