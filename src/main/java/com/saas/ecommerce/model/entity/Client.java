package com.saas.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String password; // Hashed
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean active = true;
}
