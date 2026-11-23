package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateOutcomeDocumentRequest;
import com.warehouse.demo.dto.request.OutcomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateOutcomeDocumentRequest;
import com.warehouse.demo.dto.response.OutcomeDocumentJournalResponse;
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
class OutcomeDocumentServiceImplTest {

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
    private static final String DELETED_DOCUMENT_MESSAGE = "Нельзя изменять строки помеченной на удаление реализации";

    @Mock
    private OutcomeDocumentRepository outcomeDocumentRepository;

    @Mock
    private OutcomeDocumentLineRepository outcomeDocumentLineRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutcomeDocumentMapper outcomeDocumentMapper;

    @InjectMocks
    private OutcomeDocumentServiceImpl outcomeDocumentService;

    @Test
    @DisplayName("Find all returns the page mapped into a list response")
    void givenStoredDocuments_whenFindAll_thenReturnsMappedPage() {
        // given
        Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
        OutcomeDocument storedDocument = document(DOCUMENT_ID, NOT_DELETED);
        List<OutcomeDocument> documents = List.of(storedDocument);
        OutcomeDocumentJournalResponse journalResponse = journal(DOCUMENT_ID);
        List<OutcomeDocumentJournalResponse> journal = List.of(journalResponse);
        Page<OutcomeDocument> page = mock(Page.class);
        doReturn(page).when(outcomeDocumentRepository).findAll(any(Specification.class), eq(pageable));
        when(page.getContent()).thenReturn(documents);
        when(page.getNumber()).thenReturn(PAGE_NUMBER);
        when(page.getSize()).thenReturn(PAGE_SIZE);
        when(page.getTotalElements()).thenReturn(TOTAL_ELEMENTS);
        when(page.getTotalPages()).thenReturn(TOTAL_PAGES);
        when(outcomeDocumentMapper.toJournalResponseList(documents)).thenReturn(journal);

        // when
        OutcomeDocumentListResponse result = outcomeDocumentService.findAll(pageable);

        // then
        assertSame(journal, result.outcomeDocuments());
        assertEquals(PAGE_NUMBER, result.page());
        assertEquals(PAGE_SIZE, result.size());
        assertEquals(TOTAL_ELEMENTS, result.totalElements());
        assertEquals(TOTAL_PAGES, result.totalPages());
    }

    @Test
    @DisplayName("Find by id returns the mapped document")
    void givenStoredDocument_whenFindById_thenReturnsMappedResponse() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentResponse response = response(DOCUMENT_ID);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        OutcomeDocumentResponse result = outcomeDocumentService.findById(DOCUMENT_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<OutcomeDocument> missing = Optional.empty();
        when(outcomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.findById(MISSING_DOCUMENT_ID)
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
        OutcomeDocument saved = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentResponse response = response(DOCUMENT_ID);
        CreateOutcomeDocumentRequest request = new CreateOutcomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        ArgumentCaptor<OutcomeDocument> captor = ArgumentCaptor.forClass(OutcomeDocument.class);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);
        doReturn(saved).when(outcomeDocumentRepository).saveAndFlush(captor.capture());
        when(outcomeDocumentRepository.save(saved)).thenReturn(saved);
        when(outcomeDocumentMapper.toResponse(saved)).thenReturn(response);

        // when
        OutcomeDocumentResponse result = outcomeDocumentService.save(request);

        // then
        OutcomeDocument stored = captor.getValue();
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
        OutcomeDocument saved = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentResponse response = response(DOCUMENT_ID);
        CreateOutcomeDocumentRequest request = new CreateOutcomeDocumentRequest(WAREHOUSE_ID, null);
        ArgumentCaptor<OutcomeDocument> captor = ArgumentCaptor.forClass(OutcomeDocument.class);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);
        doReturn(saved).when(outcomeDocumentRepository).saveAndFlush(captor.capture());
        when(outcomeDocumentRepository.save(saved)).thenReturn(saved);
        when(outcomeDocumentMapper.toResponse(saved)).thenReturn(response);

        // when
        outcomeDocumentService.save(request);

        // then
        OutcomeDocument stored = captor.getValue();
        LocalDateTime storedDate = stored.getDocumentDate();
        assertNotNull(storedDate);
    }

    @Test
    @DisplayName("Save fails when the warehouse does not exist")
    void givenMissingWarehouse_whenSave_thenDoesNotSave() {
        // given
        Optional<Warehouse> missingWarehouse = Optional.empty();
        CreateOutcomeDocumentRequest request = new CreateOutcomeDocumentRequest(MISSING_WAREHOUSE_ID, DOCUMENT_DATE);
        when(warehouseRepository.findById(MISSING_WAREHOUSE_ID)).thenReturn(missingWarehouse);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.save(request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_WAREHOUSE_ID);
        assertTrue(message.contains(missingId));
        verify(outcomeDocumentRepository, never()).saveAndFlush(any(OutcomeDocument.class));
    }

    @Test
    @DisplayName("Save fails when the warehouse is marked deleted")
    void givenDeletedWarehouse_whenSave_thenDoesNotSave() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, DELETED);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        CreateOutcomeDocumentRequest request = new CreateOutcomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.save(request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_WAREHOUSE_MESSAGE));
        verify(outcomeDocumentRepository, never()).saveAndFlush(any(OutcomeDocument.class));
    }

    @Test
    @DisplayName("Update stores the warehouse and the document date")
    void givenStoredDocument_whenUpdate_thenSavesDocument() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_DELETED);
        OutcomeDocumentResponse response = response(DOCUMENT_ID);
        UpdateOutcomeDocumentRequest request = new UpdateOutcomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);
        when(outcomeDocumentRepository.save(document)).thenReturn(document);
        when(outcomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        OutcomeDocumentResponse result = outcomeDocumentService.update(DOCUMENT_ID, request);

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
        Optional<OutcomeDocument> missing = Optional.empty();
        UpdateOutcomeDocumentRequest request = new UpdateOutcomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        when(outcomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.update(MISSING_DOCUMENT_ID, request)
        );

        // then
        verify(outcomeDocumentRepository, never()).save(any(OutcomeDocument.class));
    }

    @Test
    @DisplayName("Update fails when the warehouse does not exist")
    void givenMissingWarehouse_whenUpdate_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<Warehouse> missingWarehouse = Optional.empty();
        UpdateOutcomeDocumentRequest request = new UpdateOutcomeDocumentRequest(MISSING_WAREHOUSE_ID, DOCUMENT_DATE);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(warehouseRepository.findById(MISSING_WAREHOUSE_ID)).thenReturn(missingWarehouse);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.update(DOCUMENT_ID, request)
        );

        // then
        verify(outcomeDocumentRepository, never()).save(any(OutcomeDocument.class));
    }

    @Test
    @DisplayName("Update fails when the warehouse is marked deleted")
    void givenDeletedWarehouse_whenUpdate_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Warehouse warehouse = warehouse(WAREHOUSE_ID, DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<Warehouse> foundWarehouse = Optional.of(warehouse);
        UpdateOutcomeDocumentRequest request = new UpdateOutcomeDocumentRequest(WAREHOUSE_ID, DOCUMENT_DATE);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(foundWarehouse);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.update(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_WAREHOUSE_MESSAGE));
        verify(outcomeDocumentRepository, never()).save(any(OutcomeDocument.class));
    }

    @Test
    @DisplayName("A document is marked deleted")
    void givenStoredDocument_whenMarkDeleted_thenSavesMarkedDocument() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentResponse response = response(DOCUMENT_ID);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentRepository.save(document)).thenReturn(document);
        when(outcomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        outcomeDocumentService.markAsDeleted(DOCUMENT_ID);

        // then
        Boolean deleted = document.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(outcomeDocumentRepository).save(document);
    }

    @Test
    @DisplayName("Unmarking a document clears the deleted flag")
    void givenDeletedDocument_whenUnmark_thenClearsDeleted() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, DELETED);
        OutcomeDocumentResponse response = response(DOCUMENT_ID);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentRepository.save(document)).thenReturn(document);
        when(outcomeDocumentMapper.toResponse(document)).thenReturn(response);

        // when
        OutcomeDocumentResponse result = outcomeDocumentService.unmarkAsDeleted(DOCUMENT_ID);

        // then
        Boolean deleted = document.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(outcomeDocumentRepository).save(document);
    }

    @Test
    @DisplayName("A document is deleted from the repository")
    void givenStoredDocument_whenDelete_thenDeletesDocument() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        outcomeDocumentService.delete(DOCUMENT_ID);

        // then
        verify(outcomeDocumentRepository).delete(document);
    }

    @Test
    @DisplayName("Add line stores the product and the quantity")
    void givenWritableDocument_whenAddLine_thenSavesLine() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Product product = product(PRODUCT_ID, NOT_DELETED);
        OutcomeDocumentLine savedLine = line(LINE_ID);
        OutcomeDocumentLineResponse lineResponse = lineResponse();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<Product> foundProduct = Optional.of(product);
        ArgumentCaptor<OutcomeDocumentLine> captor = ArgumentCaptor.forClass(OutcomeDocumentLine.class);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);
        doReturn(savedLine).when(outcomeDocumentLineRepository).save(captor.capture());
        when(outcomeDocumentMapper.toLineResponse(savedLine)).thenReturn(lineResponse);

        // when
        OutcomeDocumentLineResponse result = outcomeDocumentService.addLine(DOCUMENT_ID, request);

        // then
        OutcomeDocumentLine storedLine = captor.getValue();
        Product storedProduct = storedLine.getProduct();
        BigDecimal storedQuantity = storedLine.getQuantity();
        List<OutcomeDocumentLine> lines = document.getLines();
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
        OutcomeDocument document = document(DOCUMENT_ID, DELETED);
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.addLine(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_DOCUMENT_MESSAGE));
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Add line fails when the document does not exist")
    void givenMissingDocument_whenAddLine_thenDoesNotSave() {
        // given
        Optional<OutcomeDocument> missing = Optional.empty();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.addLine(MISSING_DOCUMENT_ID, request)
        );

        // then
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Add line fails when the product does not exist")
    void givenMissingProduct_whenAddLine_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<Product> missingProduct = Optional.empty();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(MISSING_PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(productRepository.findById(MISSING_PRODUCT_ID)).thenReturn(missingProduct);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.addLine(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_PRODUCT_ID);
        assertTrue(message.contains(missingId));
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Add line is rejected when the product is marked deleted")
    void givenDeletedProduct_whenAddLine_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Product product = product(PRODUCT_ID, DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<Product> foundProduct = Optional.of(product);
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.addLine(DOCUMENT_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_PRODUCT_MESSAGE));
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line stores the product and the quantity")
    void givenWritableLine_whenUpdateLine_thenSavesLine() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentLine line = attachedLine(document, LINE_ID);
        Product product = product(PRODUCT_ID, NOT_DELETED);
        OutcomeDocumentLineResponse lineResponse = lineResponse();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<OutcomeDocumentLine> foundLine = Optional.of(line);
        Optional<Product> foundProduct = Optional.of(product);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);
        when(outcomeDocumentLineRepository.save(line)).thenReturn(line);
        when(outcomeDocumentMapper.toLineResponse(line)).thenReturn(lineResponse);

        // when
        OutcomeDocumentLineResponse result = outcomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request);

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
        OutcomeDocument document = document(DOCUMENT_ID, DELETED);
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_DOCUMENT_MESSAGE));
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line fails when the document does not exist")
    void givenMissingDocument_whenUpdateLine_thenDoesNotSave() {
        // given
        Optional<OutcomeDocument> missing = Optional.empty();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.updateLine(MISSING_DOCUMENT_ID, LINE_ID, request)
        );

        // then
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line fails when the line does not exist")
    void givenMissingLine_whenUpdateLine_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<OutcomeDocumentLine> missingLine = Optional.empty();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentLineRepository.findByIdAndDocument_Id(MISSING_LINE_ID, DOCUMENT_ID)).thenReturn(missingLine);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.updateLine(DOCUMENT_ID, MISSING_LINE_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingLineId = Long.toString(MISSING_LINE_ID);
        String documentId = Long.toString(DOCUMENT_ID);
        assertTrue(message.contains(missingLineId));
        assertTrue(message.contains(documentId));
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line fails when the product does not exist")
    void givenMissingProduct_whenUpdateLine_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentLine line = attachedLine(document, LINE_ID);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<OutcomeDocumentLine> foundLine = Optional.of(line);
        Optional<Product> missingProduct = Optional.empty();
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(MISSING_PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);
        when(productRepository.findById(MISSING_PRODUCT_ID)).thenReturn(missingProduct);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request)
        );

        // then
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Update line is rejected when the product is marked deleted")
    void givenDeletedProduct_whenUpdateLine_thenDoesNotSave() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentLine line = attachedLine(document, LINE_ID);
        Product product = product(PRODUCT_ID, DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<OutcomeDocumentLine> foundLine = Optional.of(line);
        Optional<Product> foundProduct = Optional.of(product);
        OutcomeDocumentLineRequest request = new OutcomeDocumentLineRequest(PRODUCT_ID, LINE_QUANTITY);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(foundProduct);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.updateLine(DOCUMENT_ID, LINE_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_PRODUCT_MESSAGE));
        verify(outcomeDocumentLineRepository, never()).save(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Delete line removes it from the document and the repository")
    void givenWritableLine_whenDeleteLine_thenDeletesLine() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        OutcomeDocumentLine line = attachedLine(document, LINE_ID);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<OutcomeDocumentLine> foundLine = Optional.of(line);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentLineRepository.findByIdAndDocument_Id(LINE_ID, DOCUMENT_ID)).thenReturn(foundLine);

        // when
        outcomeDocumentService.deleteLine(DOCUMENT_ID, LINE_ID);

        // then
        List<OutcomeDocumentLine> lines = document.getLines();
        boolean lineRemains = lines.contains(line);
        assertTrue(!lineRemains);
        verify(outcomeDocumentLineRepository).delete(line);
    }

    @Test
    @DisplayName("Delete line is rejected when the document is marked deleted")
    void givenDeletedDocument_whenDeleteLine_thenDoesNotDelete() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> outcomeDocumentService.deleteLine(DOCUMENT_ID, LINE_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETED_DOCUMENT_MESSAGE));
        verify(outcomeDocumentLineRepository, never()).delete(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Delete line fails when the document does not exist")
    void givenMissingDocument_whenDeleteLine_thenDoesNotDelete() {
        // given
        Optional<OutcomeDocument> missing = Optional.empty();
        when(outcomeDocumentRepository.findById(MISSING_DOCUMENT_ID)).thenReturn(missing);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.deleteLine(MISSING_DOCUMENT_ID, LINE_ID)
        );

        // then
        verify(outcomeDocumentLineRepository, never()).delete(any(OutcomeDocumentLine.class));
    }

    @Test
    @DisplayName("Delete line fails when the line does not exist")
    void givenMissingLine_whenDeleteLine_thenDoesNotDelete() {
        // given
        OutcomeDocument document = document(DOCUMENT_ID, NOT_DELETED);
        Optional<OutcomeDocument> found = Optional.of(document);
        Optional<OutcomeDocumentLine> missingLine = Optional.empty();
        when(outcomeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(found);
        when(outcomeDocumentLineRepository.findByIdAndDocument_Id(MISSING_LINE_ID, DOCUMENT_ID)).thenReturn(missingLine);

        // when
        assertThrows(
                EntityNotFoundException.class,
                () -> outcomeDocumentService.deleteLine(DOCUMENT_ID, MISSING_LINE_ID)
        );

        // then
        verify(outcomeDocumentLineRepository, never()).delete(any(OutcomeDocumentLine.class));
    }

    private static OutcomeDocument document(Long id, boolean deleted) {
        OutcomeDocument document = new OutcomeDocument();
        document.setId(id);
        document.setDeleted(deleted);
        document.setNumber(DOCUMENT_NUMBER);
        document.setDocumentDate(DOCUMENT_DATE);
        return document;
    }

    private static OutcomeDocumentLine attachedLine(OutcomeDocument document, Long lineId) {
        OutcomeDocumentLine line = line(lineId);
        line.setDocument(document);
        document.getLines().add(line);
        return line;
    }

    private static OutcomeDocumentLine line(Long id) {
        OutcomeDocumentLine line = new OutcomeDocumentLine();
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

    private static OutcomeDocumentResponse response(Long id) {
        List<OutcomeDocumentLineResponse> lines = List.of();
        return new OutcomeDocumentResponse(
                id,
                DOCUMENT_NUMBER,
                DOCUMENT_DATE,
                WAREHOUSE_ID,
                WAREHOUSE_NAME,
                NOT_DELETED,
                lines
        );
    }

    private static OutcomeDocumentJournalResponse journal(Long id) {
        return new OutcomeDocumentJournalResponse(
                id,
                DOCUMENT_NUMBER,
                DOCUMENT_DATE,
                WAREHOUSE_ID,
                WAREHOUSE_NAME,
                NOT_DELETED
        );
    }

    private static OutcomeDocumentLineResponse lineResponse() {
        return new OutcomeDocumentLineResponse(
                LINE_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                PRODUCT_ARTICLE,
                LINE_QUANTITY
        );
    }
}
