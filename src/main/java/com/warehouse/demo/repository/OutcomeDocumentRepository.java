package com.warehouse.demo.repository;

import com.warehouse.demo.model.OutcomeDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutcomeDocumentRepository extends JpaRepository<OutcomeDocument, Long>, JpaSpecificationExecutor<OutcomeDocument> {

    @Override
    @EntityGraph(attributePaths = {"warehouse", "lines", "lines.product"})
    Optional<OutcomeDocument> findById(Long id);
}
