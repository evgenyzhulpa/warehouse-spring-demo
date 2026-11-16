package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateOutcomeDocumentRequest;
import com.warehouse.demo.dto.request.OutcomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateOutcomeDocumentRequest;
import com.warehouse.demo.dto.response.OutcomeDocumentLineResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentListResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.OutcomeDocumentMapper;
import com.warehouse.demo.model.OutcomeDocument;
import com.warehouse.demo.model.OutcomeDocumentLine;
import com.warehouse.demo.model.Product;
import com.warehouse.demo.model.Warehouse;
import com.warehouse.demo.repository.OutcomeDocumentLineRepository;
import com.warehouse.demo.repository.OutcomeDocumentRepository;
import com.warehouse.demo.repository.ProductRepository;
import com.warehouse.demo.repository.WarehouseRepository;
import com.warehouse.demo.service.OutcomeDocumentService;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutcomeDocumentServiceImpl implements OutcomeDocumentService {

    private final OutcomeDocumentRepository outcomeDocumentRepository;
    private final OutcomeDocumentLineRepository outcomeDocumentLineRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final OutcomeDocumentMapper outcomeDocumentMapper;

    @Transactional(readOnly = true)
    @Override
    public OutcomeDocumentListResponse findAll(Pageable pageable) {
        Specification<OutcomeDocument> withWarehouse = (root, query, cb) -> {
            if (query != null && OutcomeDocument.class.equals(query.getResultType())) {
                root.fetch("warehouse", JoinType.LEFT);
            }
            return cb.conjunction();
        };
        Page<OutcomeDocument> page = outcomeDocumentRepository.findAll(withWarehouse, pageable);
        return new OutcomeDocumentListResponse(
                outcomeDocumentMapper.toJournalResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public OutcomeDocumentResponse findById(Long id) {
        return outcomeDocumentMapper.toResponse(findDocumentById(id));
    }

    @Transactional
    @Override
    public OutcomeDocumentResponse save(CreateOutcomeDocumentRequest request) {
        OutcomeDocument document = new OutcomeDocument();
        document.setWarehouse(findWritableWarehouse(request.warehouseId()));
        document.setDocumentDate(request.documentDate() != null ? request.documentDate() : LocalDateTime.now());
        document.setNumber(placeholderNumber());
        OutcomeDocument saved = outcomeDocumentRepository.saveAndFlush(document);
        saved.setNumber(String.format("%06d", saved.getId()));
        return outcomeDocumentMapper.toResponse(outcomeDocumentRepository.save(saved));
    }

    @Transactional
    @Override
    public OutcomeDocumentResponse update(Long id, UpdateOutcomeDocumentRequest request) {
        OutcomeDocument document = findDocumentById(id);
        document.setWarehouse(findWritableWarehouse(request.warehouseId()));
        document.setDocumentDate(request.documentDate());
        return outcomeDocumentMapper.toResponse(outcomeDocumentRepository.save(document));
    }

    @Transactional
    @Override
    public OutcomeDocumentResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    @Transactional
    @Override
    public OutcomeDocumentResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        OutcomeDocument document = findDocumentById(id);
        outcomeDocumentRepository.delete(document);
    }

    @Transactional
    @Override
    public OutcomeDocumentLineResponse addLine(Long documentId, OutcomeDocumentLineRequest request) {
        OutcomeDocument document = findWritableDocument(documentId);
        OutcomeDocumentLine line = new OutcomeDocumentLine();
        line.setDocument(document);
        line.setProduct(findWritableProduct(request.productId()));
        line.setQuantity(request.quantity());
        document.getLines().add(line);
        return outcomeDocumentMapper.toLineResponse(outcomeDocumentLineRepository.save(line));
    }

    @Transactional
    @Override
    public OutcomeDocumentLineResponse updateLine(Long documentId, Long lineId, OutcomeDocumentLineRequest request) {
        findWritableDocument(documentId);
        OutcomeDocumentLine line = findLine(documentId, lineId);
        line.setProduct(findWritableProduct(request.productId()));
        line.setQuantity(request.quantity());
        return outcomeDocumentMapper.toLineResponse(outcomeDocumentLineRepository.save(line));
    }

    @Transactional
    @Override
    public void deleteLine(Long documentId, Long lineId) {
        findWritableDocument(documentId);
        OutcomeDocumentLine line = findLine(documentId, lineId);
        line.getDocument().getLines().remove(line);
        outcomeDocumentLineRepository.delete(line);
    }

    private OutcomeDocumentResponse setDeletedValue(Long id, boolean deleted) {
        OutcomeDocument document = findDocumentById(id);
        document.setDeleted(deleted);
        return outcomeDocumentMapper.toResponse(outcomeDocumentRepository.save(document));
    }

    private OutcomeDocument findDocumentById(Long id) {
        return outcomeDocumentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Реализация с ID {0} не найдена!", id)
                ));
    }

    private OutcomeDocument findWritableDocument(Long id) {
        OutcomeDocument document = findDocumentById(id);
        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessLogicException("Нельзя изменять строки помеченной на удаление реализации");
        }
        return document;
    }

    private OutcomeDocumentLine findLine(Long documentId, Long lineId) {
        return outcomeDocumentLineRepository.findByIdAndDocument_Id(lineId, documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Строка {0} реализации {1} не найдена!", lineId, documentId)
                ));
    }

    private Warehouse findWritableWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Склад с ID {0} не найден!", warehouseId)
                ));
        if (Boolean.TRUE.equals(warehouse.getDeleted())) {
            throw new BusinessLogicException("Нельзя выбрать помеченный на удаление склад");
        }
        return warehouse;
    }

    private Product findWritableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Номенклатура с ID {0} не найдена!", productId)
                ));
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessLogicException("Нельзя выбрать помеченную на удаление номенклатуру");
        }
        return product;
    }

    private String placeholderNumber() {
        return "TMP-" + java.util.UUID.randomUUID();
    }
}
