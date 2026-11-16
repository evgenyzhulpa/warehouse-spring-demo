package com.warehouse.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@MappedSuperclass
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class DocumentEntity extends BaseEntity {

    @Column(name = "document_date", nullable = false)
    private LocalDateTime documentDate;

    @Column(name = "number", nullable = false, unique = true)
    private String number;
}
