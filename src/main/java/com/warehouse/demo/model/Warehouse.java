package com.warehouse.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "warehouses")
public class Warehouse extends SystemEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "address")
    @ToString.Exclude
    private String address;
}
