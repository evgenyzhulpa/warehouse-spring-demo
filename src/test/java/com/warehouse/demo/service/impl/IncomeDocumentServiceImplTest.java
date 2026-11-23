package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateIncomeDocumentRequest;
import com.warehouse.demo.dto.request.IncomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateIncomeDocumentRequest;
import com.warehouse.demo.dto.response.IncomeDocumentJournalResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeDocumentServiceImplTest {

    private static final long DOCUMENT_ID = 1L;
    private static final long MISSING_DOCUMENT_ID = 5L;
    private static final long LINE_ID = 2L;
    private static final long MISSING_LINE_ID = 8L;
    private static final long WAREHOUSE_ID = 3L;
    private static final long MISSING_WAREHOUSE_ID = 6L;
    private static final long PRODUCT_ID = 4L;
    private static final long MISSING_PRODUCT_ID = 7L;
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 20;
    private static final long TOTAL_ELEMENTS = 1L;
    private static final int TOTAL_PAGES = 1;
    private static final boolean DELETED = true;
    private static final boolean NOT_DELETED = false;
    private static final String DOCUMENT_NUMBER = "000001";
    private static final String WAREHOUSE_NAME = "Main";
    private static final String PRODUCT_NAME = "Bolt";
    private static final String PRODUCT_ARTICLE = "BLT-1";
    private static final LocalDateTime DOCUMENT_DATE = LocalDateTime.of(2026, 11, 23, 8, 0);
    private static final BigDecimal LINE_QUANTITY = new BigDecimal("1.500");
    private static final String DELETED_WAREHOUSE_MESSAGE = "Нельзя выбрать помеченный на удаление склад";
    private static final String DELETED_PRODUCT_MESSAGE = "Нельзя выбрать помеченную на удаление номенклатуру";
    private static final String DELETED_DOCUMENT_MESSAGE = "Нельзя изменять строки помеченного на удаление поступления";

    @Mock
    private IncomeDocumentRepository incomeDocumentRepository;

    @Mock
    private IncomeDocumentLineRepository incomeDocumentLineRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IncomeDocumentMapper incomeDocumentMapper;

    @InjectMocks
    private IncomeDocumentServiceImpl incomeDocumentService;

    @Test
    @DisplayName("Find all returns the page mapped into a list response")
    void givenStoredDocuments_whenFindAll_thenReturnsMappedPage() {
        // given
        Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
        IncomeDocument storedDocument = document(DOCUMENT_ID, NOT_DELETED);
        List<IncomeDocument> documents = List.of(storedDocument);
        IncomeDocumentJournalResponse journalResponse = journal(DOCUMENT_ID);
        List<IncomeDocumentJournalResponse> journal = List.of(journalResponse);
        Page<IncomeDocument> page = mock(Page.class);
        doReturn(page).when(incomeDocumentRepository).findAll(any(Specification.class), eq(pageable));
        when(page.getContent()).thenReturn(documents);
        when(page.getNumber()).thenReturn(PAGE_NUMBER);
        when(page.getSize()).thenReturn(PAGE_SIZE);
        when(page.getTotalElements()).thenReturn(TOTAL_ELEMENTS);
        when(page.getTotalPages()).thenReturn(TOTAL_PAGES);
        when(incomeDocumentMapper.toJournalResponseList(documents)).thenReturn(journal);

        // when
        IncomeDocumentListResponse result = incomeDocumentService.findAll(pageable);

        // then
        assertSame(journal, result.incomeDocuments());
        assertEquals(PAGE_NUMBER, result.page());
        assertEquals(PAGE_SIZE, result.size());
        assertEquals(TOTAL_ELEMENTS, result.totalElements());
        assertEquals(TOTAL_PAGES, result.totalPages());
    }

    @Test
    @DisplayName("Find by id returns the mapped document")
    void givenStoredDocument_whenFindById_thenReturnsMappedResponse() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentResponse response = response(DOCUMENT_ID);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        IncomeDocumentResponse result = incomeDocumentService.findById(DOCUMENT_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<IncomeDocument> missing = Optional.empty();
        when(incomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.findById(MISSING_DOCUMENT_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_DOCUMENT_ID);
        assertTrue(message.contains(missingId));
    }

    @Test
    @DisplayName("Save stores the warehouse, the given date and a number from the id")
    void givenCreateRequest_whenSave_thenReturnsMappedResponse() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_DELETED);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        IncomeDocument saved = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentResponse response = response(DOCUMENT_ID);
        CreateIncomeDocumentRequest request = new CreateIncomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        ArgumentCaptor<IncomeDocument> captor = ArgumentCaptor.forClass(IncomeDocument.class);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);
        doReturn(saved).when(incomeDocumentRepository).saveAndFlush(captor.capture());
        when(incomeDocumentRepository.save(saved)).thenReturn(saved);
        when(incomeDocumentMapper.toResponse(saved)).thenReturn(response);

        // when
        IncomeDocumentResponse result = incomeDocumentService.save(request);

        // then
        IncomeDocument stored = captor.getValue();
        Warehouse storedWarehouse = stored.getWarehouse();
        LocalDateTime storedDate = stored.getDocumentDate();
        String number = saved.getNumber();
        assertSame(response, result);
        assertSame(warehouse, storedWarehouse);
        assertEquals(DOCUMENT_DATE, storedDate);
        assertEquals(DOCUMENT_NUMBER, number);
    }

    @Test
    @DisplayName("Save without a date fills the document date")
    void givenRequestWithoutDate_whenSave_thenFillsDocumentDate() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_DELETED);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        IncomeDocument saved = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentResponse response = response(DOCUMENT_ID);
        CreateIncomeDocumentRequest request = new CreateIncomeDocumentRequest(WAREHOUSE_ID, null);
        ArgumentCaptor<IncomeDocument> captor = ArgumentCaptor.forClass(IncomeDocument.class);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);
        doReturn(saved).when(incomeDocumentRepository).saveAndFlush(captor.capture());
        when(incomeDocumentRepository.save(saved)).thenReturn(saved);
        when(incomeDocumentMapper.toResponse(saved)).thenReturn(response);

        // when
        incomeDocumentService.save(request);

        // then
        IncomeDocument stored = captor.getValue();
        LocalDateTime storedDate = stored.getDocumentDate();
        assertNotNull(storedDate);
    }

    @Test
    @DisplayName("Save fails when the warehouse does not exist")
    void givenMissingWarehouse_whenSave_thenDoesNotSave() {
        // given
        Optional<Warehouse> missingWarehouse = Optional.empty();
        CreateIncomeDocumentRequest request = new CreateIncomeDocumentRequest(MISSING_WAREHOUSE_ID, DOCUMENT_DATE);
        when(warehouseRepository.findById(MISSING_WAREHOUSE_ID)).thenReturn(missingWarehouse);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.save(request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_WAREHOUSE_ID);
        assertTrue(message.contains(missingId));
        verify(incomeDocumentRepository, never()).saveAndFlush(any(IncomeDocument.class));
    }

    @Test
    @DisplayName("Save fails when the warehouse is marked deleted")
    void givenDeletedWarehouse_whenSave_thenDoesNotSave() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, DELETED);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        CreateIncomeDocumentRequest request = new CreateIncomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.save(request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_WAREHOUSE_MESSAGE));
        verify(incomeDocumentRepository, never()).saveAndFlush(any(IncomeDocument.class));
    }

    @Test
    @DisplayName("Update stores the warehouse and the document date")
    void givenStoredDocument_whenUpdate_thenSavesDocument() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_DELETED);
        IncomeDocumentResponse response = response(DOCUMENT_ID);
        UpdateIncomeDocumentRequest request = new UpdateIncomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);
        when(incomeDocumentRepository.save(document)).thenReturn(document);
        when(incomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        IncomeDocumentResponse result = incomeDocumentService.update(DOCUMENT_ID, request);

        // then
        Warehouse storedWarehouse = document.getWarehouse();
        LocalDateTime storedDate = document.getDocumentDate();
        assertSame(response, result);
        assertSame(warehouse, storedWarehouse);
        assertEquals(DOCUMENT_DATE, storedDate);
    }

    @Test
    @DisplayName("Update fails when the document does not exist")
    void givenMissingDocument_whenUpdate_thenDoesNotSave() {
        // given
        Optional<IncomeDocument> missing = Optional.empty();
        UpdateIncomeDocumentRequest request = new UpdateIncomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        when(incomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.update(MISSING_DOCUMENT_ID, request)
        );

        // then
        verify(incomeDocumentRepository, never()).save(any(IncomeDocument.class));
    }

    @Test
    @DisplayName("Update fails when the warehouse does not exist")
    void givenMissingWarehouse_whenUpdate_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<Warehouse> missingWarehouse = Optional.empty();
        UpdateIncomeDocumentRequest request = new UpdateIncomeDocumentRequest(MISSING_WAREHOUSE_ID, DOCUMENT_DATE);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(warehouseRepository.findById(MISSING_WAREHOUSE_ID)).thenReturn(missingWarehouse);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.update(DOCUMENT_ID, request)
        );

        // then
        verify(incomeDocumentRepository, never()).save(any(IncomeDocument.class));
    }

    @Test
    @DisplayName("Update fails when the warehouse is marked deleted")
    void givenDeletedWarehouse_whenUpdate_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Warehouse warehouse = warehouse(WAREHOUSE_ID, DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        UpdateIncomeDocumentRequest request = new UpdateIncomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.update(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_WAREHOUSE_MESSAGE));
        verify(incomeDocumentRepository, never()).save(any(IncomeDocument.class));
    }

    @Test
    @DisplayName("A document is marked deleted")
    void givenStoredDocument_whenMarkDeleted_thenSavesMarkedDocument() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentResponse response = response(DOCUMENT_ID);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentRepository.save(document)).thenReturn(document);
        when(incomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        incomeDocumentService.markAsDeleted(DOCUMENT_ID);

        // then
        Boolean deleted = document.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(incomeDocumentRepository).save(document);
    }

    @Test
    @DisplayName("Unmarking a document clears the deleted flag")
    void givenDeletedDocument_whenUnmark_thenClearsDeleted() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, DELETED);
        IncomeDocumentResponse response = response(DOCUMENT_ID);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentRepository.save(document)).thenReturn(document);
        when(incomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        IncomeDocumentResponse result = incomeDocumentService.unmarkAsDeleted(DOCUMENT_ID);

        // then
        Boolean deleted = document.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(incomeDocumentRepository).save(document);
    }

    @Test
    @DisplayName("A document is deleted from the repository")
    void givenStoredDocument_whenDelete_thenDeletesDocument() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        incomeDocumentService.delete(DOCUMENT_ID);

        // then
        verify(incomeDocumentRepository).delete(document);
    }

    @Test
    @DisplayName("Add line stores the product and the quantity")
    void givenWritableDocument_whenAddLine_thenSavesLine() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Product product = product(PRODUCT_ID, NOT_DELETED);
        IncomeDocumentLine savedLine = line(LINE_ID);
        IncomeDocumentLineResponse lineResponse = lineResponse();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<Product> foundProduct = Optional.of(product);
        ArgumentCaptor<IncomeDocumentLine> captor = ArgumentCaptor.forClass(IncomeDocumentLine.class);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);
        doReturn(savedLine).when(incomeDocumentLineRepository).save(captor.capture());
        when(incomeDocumentMapper.toLineResponse(savedLine)).thenReturn(lineResponse);

        // when
        IncomeDocumentLineResponse result = incomeDocumentService.addLine(DOCUMENT_ID, request);

        // then
        IncomeDocumentLine storedLine = captor.getValue();
        Product storedProduct = storedLine.getProduct();
        BigDecimal storedQuantity = storedLine.getQuantity();
        List<IncomeDocumentLine> lines = document.getLines();
        boolean lineAdded = lines.contains(storedLine);
        assertSame(lineResponse, result);
        assertSame(product, storedProduct);
        assertEquals(LINE_QUANTITY, storedQuantity);
        assertTrue(lineAdded);
    }

    @Test
    @DisplayName("Add line is rejected when the document is marked deleted")
    void givenDeletedDocument_whenAddLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, DELETED);
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.addLine(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_DOCUMENT_MESSAGE));
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Add line fails when the document does not exist")
    void givenMissingDocument_whenAddLine_thenDoesNotSave() {
        // given
        Optional<IncomeDocument> missing = Optional.empty();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.addLine(MISSING_DOCUMENT_ID, request)
        );

        // then
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Add line fails when the product does not exist")
    void givenMissingProduct_whenAddLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<Product> missingProduct = Optional.empty();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(MISSING_PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(productRepository.findById(MISSING_PRODUCT_ID)).thenReturn(missingProduct);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.addLine(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_PRODUCT_ID);
        assertTrue(message.contains(missingId));
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Add line is rejected when the product is marked deleted")
    void givenDeletedProduct_whenAddLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Product product = product(PRODUCT_ID, DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<Product> foundProduct = Optional.of(product);
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.addLine(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_PRODUCT_MESSAGE));
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line stores the product and the quantity")
    void givenWritableLine_whenUpdateLine_thenSavesLine() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentLine line = attachedLine(document, LINE_ID);
        Product product = product(PRODUCT_ID, NOT_DELETED);
        IncomeDocumentLineResponse lineResponse = lineResponse();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<IncomeDocumentLine> foundLine = Optional.of(line);
        Optional<Product> foundProduct = Optional.of(product);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);
        when(incomeDocumentLineRepository.save(line)).thenReturn(line);
        when(incomeDocumentMapper.toLineResponse(line)).thenReturn(lineResponse);

        // when
        IncomeDocumentLineResponse result = incomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request);

        // then
        Product storedProduct = line.getProduct();
        BigDecimal storedQuantity = line.getQuantity();
        assertSame(lineResponse, result);
        assertSame(product, storedProduct);
        assertEquals(LINE_QUANTITY, storedQuantity);
    }

    @Test
    @DisplayName("Update line is rejected when the document is marked deleted")
    void givenDeletedDocument_whenUpdateLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, DELETED);
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_DOCUMENT_MESSAGE));
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line fails when the document does not exist")
    void givenMissingDocument_whenUpdateLine_thenDoesNotSave() {
        // given
        Optional<IncomeDocument> missing = Optional.empty();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.updateLine(MISSING_DOCUMENT_ID, LINE_ID, request)
        );

        // then
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line fails when the line does not exist")
    void givenMissingLine_whenUpdateLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<IncomeDocumentLine> missingLine = Optional.empty();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentLineRepository.findByIdAndDocument_Id(MISSING_LINE_ID, DOCUMENT_ID)).thenReturn(missingLine);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.updateLine(DOCUMENT_ID, MISSING_LINE_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingLineId = Long.toString(MISSING_LINE_ID);
        String documentId = Long.toString(DOCUMENT_ID);
        assertTrue(message.contains(missingLineId));
        assertTrue(message.contains(documentId));
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line fails when the product does not exist")
    void givenMissingProduct_whenUpdateLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentLine line = attachedLine(document, LINE_ID);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<IncomeDocumentLine> foundLine = Optional.of(line);
        Optional<Product> missingProduct = Optional.empty();
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(MISSING_PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);
        when(productRepository.findById(MISSING_PRODUCT_ID)).thenReturn(missingProduct);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request)
        );

        // then
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line is rejected when the product is marked deleted")
    void givenDeletedProduct_whenUpdateLine_thenDoesNotSave() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentLine line = attachedLine(document, LINE_ID);
        Product product = product(PRODUCT_ID, DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<IncomeDocumentLine> foundLine = Optional.of(line);
        Optional<Product> foundProduct = Optional.of(product);
        IncomeDocumentLineRequest request = new IncomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_PRODUCT_MESSAGE));
        verify(incomeDocumentLineRepository, never()).save(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Delete line removes it from the document and the repository")
    void givenWritableLine_whenDeleteLine_thenDeletesLine() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        IncomeDocumentLine line = attachedLine(document, LINE_ID);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<IncomeDocumentLine> foundLine = Optional.of(line);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);

        // when
        incomeDocumentService.deleteLine(DOCUMENT_ID, LINE_ID);

        // then
        List<IncomeDocumentLine> lines = document.getLines();
        boolean lineRemains = lines.contains(line);
        assertTrue(!lineRemains);
        verify(incomeDocumentLineRepository).delete(line);
    }

    @Test
    @DisplayName("Delete line is rejected when the document is marked deleted")
    void givenDeletedDocument_whenDeleteLine_thenDoesNotDelete() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> incomeDocumentService.deleteLine(DOCUMENT_ID, LINE_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_DOCUMENT_MESSAGE));
        verify(incomeDocumentLineRepository, never()).delete(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Delete line fails when the document does not exist")
    void givenMissingDocument_whenDeleteLine_thenDoesNotDelete() {
        // given
        Optional<IncomeDocument> missing = Optional.empty();
        when(incomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.deleteLine(MISSING_DOCUMENT_ID, LINE_ID)
        );

        // then
        verify(incomeDocumentLineRepository, never()).delete(any(IncomeDocumentLine.class));
    }

    @Test
    @DisplayName("Delete line fails when the line does not exist")
    void givenMissingLine_whenDeleteLine_thenDoesNotDelete() {
        // given
        IncomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<IncomeDocument> found = Optional.of(document);
        Optional<IncomeDocumentLine> missingLine = Optional.empty();
        when(incomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(incomeDocumentLineRepository.findByIdAndDocument_Id(MISSING_LINE_ID, DOCUMENT_ID)).thenReturn(missingLine);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> incomeDocumentService.deleteLine(DOCUMENT_ID, MISSING_LINE_ID)
        );

        // then
        verify(incomeDocumentLineRepository, never()).delete(any(IncomeDocumentLine.class));
    }

    private static IncomeDocument document(Long id, boolean deleted) {
        IncomeDocument document = new IncomeDocument();
        document.setId(id);
        document.setDeleted(deleted);
        document.setNumber(DOCUMENT_NUMBER);
        document.setDocumentDate(DOCUMENT_DATE);
        return document;
    }

    private static IncomeDocumentLine attachedLine(IncomeDocument document, Long lineId) {
        IncomeDocumentLine line = line(lineId);
        line.setDocument(document);
        document.getLines().add(line);
        return line;
    }

    private static IncomeDocumentLine line(Long id) {
        IncomeDocumentLine line = new IncomeDocumentLine();
        line.setId(id);
        line.setQuantity(LINE_QUANTITY);
        return line;
    }

    private static Warehouse warehouse(Long id, boolean deleted) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName(WAREHOUSE_NAME);
        warehouse.setDeleted(deleted);
        return warehouse;
    }

    private static Product product(Long id, boolean deleted) {
        Product product = new Product();
        product.setId(id);
        product.setName(PRODUCT_NAME);
        product.setArticle(PRODUCT_ARTICLE);
        product.setDeleted(deleted);
        return product;
    }

    private static IncomeDocumentResponse response(Long id) {
        List<IncomeDocumentLineResponse> lines = List.of();
        return new IncomeDocumentResponse(
                id,
                DOCUMENT_NUMBER,
                DOCUMENT_DATE,
                WAREHOUSE_ID,
                WAREHOUSE_NAME,
                NOT_DELETED,
                lines
        );
    }

    private static IncomeDocumentJournalResponse journal(Long id) {
        return new IncomeDocumentJournalResponse(
                id,
                DOCUMENT_NUMBER,
                DOCUMENT_DATE,
                WAREHOUSE_ID,
                WAREHOUSE_NAME,
                NOT_DELETED
        );
    }

    private static IncomeDocumentLineResponse lineResponse() {
        return new IncomeDocumentLineResponse(
                LINE_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                PRODUCT_ARTICLE,
                LINE_QUANTITY
        );
    }
}
