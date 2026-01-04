package com.example.template.domain.user.entity;

import com.example.template.global.common.entity.BaseEntity;
import com.example.template.global.common.entity.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.VISITOR;

    public void changePassword(String encodedPassword){
        this.password = encodedPassword;
    }

    public void changeRole(Role role){this.role=role;}

    public void changeEmail(String email){this.email=email;}

}
