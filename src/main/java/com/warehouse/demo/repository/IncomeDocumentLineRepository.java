package com.warehouse.demo.repository;

import com.warehouse.demo.model.IncomeDocumentLine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncomeDocumentLineRepository extends JpaRepository<IncomeDocumentLine, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<IncomeDocumentLine> findByIdAndDocument_Id(Long id, Long documentId);
}
