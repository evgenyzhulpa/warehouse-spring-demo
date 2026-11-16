package com.warehouse.demo.repository;

import com.warehouse.demo.model.IncomeDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncomeDocumentRepository extends JpaRepository<IncomeDocument, Long>, JpaSpecificationExecutor<IncomeDocument> {

    @Override
    @EntityGraph(attributePaths = {"warehouse", "lines", "lines.product"})
    Optional<IncomeDocument> findById(Long id);
}
