package com.api.finance.auth.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 🆔 ID principal
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // 👤 Dados básicos
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "isAdministrator")
    private boolean isAdministrator;


    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;


}

