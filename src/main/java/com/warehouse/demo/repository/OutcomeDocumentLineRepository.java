package com.warehouse.demo.repository;

import com.warehouse.demo.model.OutcomeDocumentLine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutcomeDocumentLineRepository extends JpaRepository<OutcomeDocumentLine, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<OutcomeDocumentLine> findByIdAndDocument_Id(Long id, Long documentId);
}
