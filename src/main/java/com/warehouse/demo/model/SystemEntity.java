package com.warehouse.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;

@MappedSuperclass
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class SystemEntity extends BaseEntity {

    @Column(name = "system_defined", nullable = false)
    private Boolean systemDefined;
}
