package com.saas.ecommerce.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class RefreshToken {
    @Id
    private String id = UUID.randomUUID().toString();

    private String token;
    private LocalDateTime expiryDate;
    private boolean revoked;

    @ManyToOne
    private Client client;

    @ManyToOne
    private User user;
}
