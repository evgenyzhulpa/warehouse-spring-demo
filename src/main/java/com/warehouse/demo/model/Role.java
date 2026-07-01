package com.warehouse.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "roles")
public class Role extends SystemEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_permissions",
        joinColumns = {@JoinColumn(name = "role_id")},
        inverseJoinColumns = {@JoinColumn(name = "permission_id")})
    @ToString.Exclude
    private Set<Permission> permissions = new HashSet<>();

}

