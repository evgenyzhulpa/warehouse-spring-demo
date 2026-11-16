package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateIncomeDocumentRequest;
import com.warehouse.demo.dto.request.IncomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateIncomeDocumentRequest;
import com.warehouse.demo.dto.response.IncomeDocumentLineResponse;
import com.warehouse.demo.dto.response.IncomeDocumentListResponse;
import com.warehouse.demo.dto.response.IncomeDocumentResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.IncomeDocumentMapper;
import com.warehouse.demo.model.IncomeDocument;
import com.warehouse.demo.model.IncomeDocumentLine;
import com.warehouse.demo.model.Product;
import com.warehouse.demo.model.Warehouse;
import com.warehouse.demo.repository.IncomeDocumentLineRepository;
import com.warehouse.demo.repository.IncomeDocumentRepository;
import com.warehouse.demo.repository.ProductRepository;
import com.warehouse.demo.repository.WarehouseRepository;
import com.warehouse.demo.service.IncomeDocumentService;
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
public class IncomeDocumentServiceImpl implements IncomeDocumentService {

    private final IncomeDocumentRepository incomeDocumentRepository;
    private final IncomeDocumentLineRepository incomeDocumentLineRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final IncomeDocumentMapper incomeDocumentMapper;

    @Transactional(readOnly = true)
    @Override
    public IncomeDocumentListResponse findAll(Pageable pageable) {
        Specification<IncomeDocument> withWarehouse = (root, query, cb) -> {
            if (query != null && IncomeDocument.class.equals(query.getResultType())) {
                root.fetch("warehouse", JoinType.LEFT);
            }
            return cb.conjunction();
        };
        Page<IncomeDocument> page = incomeDocumentRepository.findAll(withWarehouse, pageable);
        return new IncomeDocumentListResponse(
                incomeDocumentMapper.toJournalResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public IncomeDocumentResponse findById(Long id) {
        return incomeDocumentMapper.toResponse(findDocumentById(id));
    }

    @Transactional
    @Override
    public IncomeDocumentResponse save(CreateIncomeDocumentRequest request) {
        IncomeDocument document = new IncomeDocument();
        document.setWarehouse(findWritableWarehouse(request.warehouseId()));
        document.setDocumentDate(request.documentDate() != null ? request.documentDate() : LocalDateTime.now());
        document.setNumber(placeholderNumber());
        IncomeDocument saved = incomeDocumentRepository.saveAndFlush(document);
        saved.setNumber(String.format("%06d", saved.getId()));
        return incomeDocumentMapper.toResponse(incomeDocumentRepository.save(saved));
    }

    @Transactional
    @Override
    public IncomeDocumentResponse update(Long id, UpdateIncomeDocumentRequest request) {
        IncomeDocument document = findDocumentById(id);
        document.setWarehouse(findWritableWarehouse(request.warehouseId()));
        document.setDocumentDate(request.documentDate());
        return incomeDocumentMapper.toResponse(incomeDocumentRepository.save(document));
    }

    @Transactional
    @Override
    public IncomeDocumentResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    @Transactional
    @Override
    public IncomeDocumentResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        IncomeDocument document = findDocumentById(id);
        incomeDocumentRepository.delete(document);
    }

    @Transactional
    @Override
    public IncomeDocumentLineResponse addLine(Long documentId, IncomeDocumentLineRequest request) {
        IncomeDocument document = findWritableDocument(documentId);
        IncomeDocumentLine line = new IncomeDocumentLine();
        line.setDocument(document);
        line.setProduct(findWritableProduct(request.productId()));
        line.setQuantity(request.quantity());
        document.getLines().add(line);
        return incomeDocumentMapper.toLineResponse(incomeDocumentLineRepository.save(line));
    }

    @Transactional
    @Override
    public IncomeDocumentLineResponse updateLine(Long documentId, Long lineId, IncomeDocumentLineRequest request) {
        findWritableDocument(documentId);
        IncomeDocumentLine line = findLine(documentId, lineId);
        line.setProduct(findWritableProduct(request.productId()));
        line.setQuantity(request.quantity());
        return incomeDocumentMapper.toLineResponse(incomeDocumentLineRepository.save(line));
    }

    @Transactional
    @Override
    public void deleteLine(Long documentId, Long lineId) {
        findWritableDocument(documentId);
        IncomeDocumentLine line = findLine(documentId, lineId);
        line.getDocument().getLines().remove(line);
        incomeDocumentLineRepository.delete(line);
    }

    private IncomeDocumentResponse setDeletedValue(Long id, boolean deleted) {
        IncomeDocument document = findDocumentById(id);
        document.setDeleted(deleted);
        return incomeDocumentMapper.toResponse(incomeDocumentRepository.save(document));
    }

    private IncomeDocument findDocumentById(Long id) {
        return incomeDocumentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Поступление с ID {0} не найдено!", id)
                ));
    }

    private IncomeDocument findWritableDocument(Long id) {
        IncomeDocument document = findDocumentById(id);
        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessLogicException("Нельзя изменять строки помеченного на удаление поступления");
        }
        return document;
    }

    private IncomeDocumentLine findLine(Long documentId, Long lineId) {
        return incomeDocumentLineRepository.findByIdAndDocument_Id(lineId, documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Строка {0} поступления {1} не найдена!", lineId, documentId)
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
