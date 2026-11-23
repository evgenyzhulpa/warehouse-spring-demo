package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateProductRequest;
import com.warehouse.demo.dto.request.UpdateProductRequest;
import com.warehouse.demo.dto.response.ProductListResponse;
import com.warehouse.demo.dto.response.ProductResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.ProductMapper;
import com.warehouse.demo.model.Product;
import com.warehouse.demo.model.Unit;
import com.warehouse.demo.repository.ProductRepository;
import com.warehouse.demo.repository.UnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class ProductServiceImplTest {

    private static final long PRODUCT_ID = 1L;
    private static final long MISSING_PRODUCT_ID = 5L;
    private static final long UNIT_ID = 3L;
    private static final long MISSING_UNIT_ID = 9L;
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 20;
    private static final long TOTAL_ELEMENTS = 1L;
    private static final int TOTAL_PAGES = 1;
    private static final boolean SYSTEM_DEFINED = true;
    private static final boolean NOT_SYSTEM_DEFINED = false;
    private static final boolean DELETED = true;
    private static final boolean NOT_DELETED = false;
    private static final String BOLT_NAME = "Bolt";
    private static final String BOLT_ARTICLE = "BLT-1";
    private static final String NUT_NAME = "Nut";
    private static final String NUT_ARTICLE = "NUT-1";
    private static final String UNIT_NAME = "Piece";
    private static final String DELETE_FORBIDDEN_MESSAGE = "Удаление предопределенной номенклатуры запрещено";

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("Find all returns the page mapped into a list response")
    void givenStoredProducts_whenFindAll_thenReturnsMappedPage() {
        // given
        Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
        Product storedProduct = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<Product> products = List.of(storedProduct);
        ProductResponse productResponse = response(PRODUCT_ID);
        List<ProductResponse> productResponses = List.of(productResponse);
        Page<Product> page = mock(Page.class);
        doReturn(page).when(productRepository).findAll(any(Specification.class), eq(pageable));
        when(page.getContent()).thenReturn(products);
        when(page.getNumber()).thenReturn(PAGE_NUMBER);
        when(page.getSize()).thenReturn(PAGE_SIZE);
        when(page.getTotalElements()).thenReturn(TOTAL_ELEMENTS);
        when(page.getTotalPages()).thenReturn(TOTAL_PAGES);
        when(productMapper.productListToProductResponseList(products)).thenReturn(productResponses);

        // when
        ProductListResponse result = productService.findAll(BOLT_NAME, BOLT_ARTICLE, pageable);

        // then
        assertSame(productResponses, result.products());
        assertEquals(PAGE_NUMBER, result.page());
        assertEquals(PAGE_SIZE, result.size());
        assertEquals(TOTAL_ELEMENTS, result.totalElements());
        assertEquals(TOTAL_PAGES, result.totalPages());
    }

    @Test
    @DisplayName("Find by id returns the mapped product")
    void givenStoredProduct_whenFindById_thenReturnsMappedResponse() {
        // given
        Product product = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        ProductResponse response = response(PRODUCT_ID);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);
        when(productMapper.productToProductResponse(product)).thenReturn(response);

        // when
        ProductResponse result = productService.findById(PRODUCT_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<Product> missing = Optional.empty();
        when(productRepository.findById(MISSING_PRODUCT_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> productService.findById(MISSING_PRODUCT_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_PRODUCT_ID);
        assertTrue(message.contains(missingId));
    }

    @Test
    @DisplayName("Save attaches the unit and returns the mapped response")
    void givenCreateRequest_whenSave_thenReturnsMappedResponse() {
        // given
        CreateProductRequest request = new CreateProductRequest(BOLT_NAME, BOLT_ARTICLE, UNIT_ID);
        Product mapped = new Product();
        Unit unit = unit(UNIT_ID);
        ProductResponse response = response(PRODUCT_ID);
        Optional<Unit> foundUnit = Optional.of(unit);
        when(productMapper.createProductRequestToProduct(request)).thenReturn(mapped);
        when(unitRepository.findById(UNIT_ID)).thenReturn(foundUnit);
        when(productRepository.save(mapped)).thenReturn(mapped);
        when(productMapper.productToProductResponse(mapped)).thenReturn(response);

        // when
        ProductResponse result = productService.save(request);

        // then
        Unit assignedUnit = mapped.getUnit();
        assertSame(response, result);
        assertSame(unit, assignedUnit);
    }

    @Test
    @DisplayName("Save fails when the unit does not exist and is not stored")
    void givenMissingUnit_whenSave_thenDoesNotSave() {
        // given
        CreateProductRequest request = new CreateProductRequest(BOLT_NAME, BOLT_ARTICLE, MISSING_UNIT_ID);
        Product mapped = new Product();
        Optional<Unit> missingUnit = Optional.empty();
        when(productMapper.createProductRequestToProduct(request)).thenReturn(mapped);
        when(unitRepository.findById(MISSING_UNIT_ID)).thenReturn(missingUnit);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> productService.save(request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_UNIT_ID);
        assertTrue(message.contains(missingId));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Update of an ordinary product is saved with the selected unit")
    void givenOrdinaryProduct_whenUpdate_thenSavesMappedProduct() {
        // given
        Product product = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Unit unit = unit(UNIT_ID);
        UpdateProductRequest request = new UpdateProductRequest(NUT_NAME, NUT_ARTICLE, UNIT_ID);
        ProductResponse response = response(PRODUCT_ID);
        Optional<Product> found = Optional.of(product);
        Optional<Unit> foundUnit = Optional.of(unit);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);
        when(unitRepository.findById(UNIT_ID)).thenReturn(foundUnit);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.productToProductResponse(product)).thenReturn(response);

        // when
        ProductResponse result = productService.update(PRODUCT_ID, request);

        // then
        Unit assignedUnit = product.getUnit();
        assertSame(response, result);
        assertSame(unit, assignedUnit);
        verify(productMapper).updateProductFromUpdateProductRequest(request, product);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Update of a system-defined product is rejected and is not saved")
    void givenSystemDefinedProduct_whenUpdate_thenDoesNotSave() {
        // given
        Product product = product(PRODUCT_ID, SYSTEM_DEFINED, NOT_DELETED);
        UpdateProductRequest request = new UpdateProductRequest(BOLT_NAME, BOLT_ARTICLE, UNIT_ID);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);

        // when
        assertThrows(
                BusinessLogicException.class,
                () -> productService.update(PRODUCT_ID, request)
        );

        // then
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Update fails when the unit does not exist and is not stored")
    void givenMissingUnit_whenUpdate_thenDoesNotSave() {
        // given
        Product product = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateProductRequest request = new UpdateProductRequest(BOLT_NAME, BOLT_ARTICLE, MISSING_UNIT_ID);
        Optional<Product> found = Optional.of(product);
        Optional<Unit> missingUnit = Optional.empty();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);
        when(unitRepository.findById(MISSING_UNIT_ID)).thenReturn(missingUnit);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> productService.update(PRODUCT_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_UNIT_ID);
        assertTrue(message.contains(missingId));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("An ordinary product is marked deleted")
    void givenOrdinaryProduct_whenMarkDeleted_thenSavesMarkedProduct() {
        // given
        Product product = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        ProductResponse response = response(PRODUCT_ID);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.productToProductResponse(product)).thenReturn(response);

        // when
        productService.markAsDeleted(PRODUCT_ID);

        // then
        Boolean deleted = product.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Marking a system-defined product deleted is rejected")
    void givenSystemDefinedProduct_whenMarkDeleted_thenDoesNotSave() {
        // given
        Product product = product(PRODUCT_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);

        // when
        assertThrows(BusinessLogicException.class, () -> productService.markAsDeleted(PRODUCT_ID));

        // then
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Unmarking an ordinary product clears the deleted flag")
    void givenDeletedOrdinaryProduct_whenUnmark_thenClearsDeleted() {
        // given
        Product product = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, DELETED);
        ProductResponse response = response(PRODUCT_ID);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.productToProductResponse(product)).thenReturn(response);

        // when
        productService.unmarkAsDeleted(PRODUCT_ID);

        // then
        Boolean deleted = product.getDeleted();
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Unmarking a system-defined product clears the deleted flag")
    void givenDeletedSystemDefinedProduct_whenUnmark_thenClearsDeleted() {
        // given
        Product product = product(PRODUCT_ID, SYSTEM_DEFINED, DELETED);
        ProductResponse response = response(PRODUCT_ID);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.productToProductResponse(product)).thenReturn(response);

        // when
        ProductResponse result = productService.unmarkAsDeleted(PRODUCT_ID);

        // then
        Boolean deleted = product.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("An ordinary product is deleted from the repository")
    void givenOrdinaryProduct_whenDelete_thenDeletesProduct() {
        // given
        Product product = product(PRODUCT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);

        // when
        productService.delete(PRODUCT_ID);

        // then
        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("Delete of a system-defined product is rejected")
    void givenSystemDefinedProduct_whenDelete_thenDoesNotDelete() {
        // given
        Product product = product(PRODUCT_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Product> found = Optional.of(product);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> productService.delete(PRODUCT_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETE_FORBIDDEN_MESSAGE));
        verify(productRepository, never()).delete(any(Product.class));
    }

    private static Product product(Long id, boolean systemDefined, boolean deleted) {
        Product product = new Product();
        product.setId(id);
        product.setName(BOLT_NAME);
        product.setArticle(BOLT_ARTICLE);
        product.setSystemDefined(systemDefined);
        product.setDeleted(deleted);
        return product;
    }

    private static Unit unit(Long id) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setName(UNIT_NAME);
        unit.setDeleted(NOT_DELETED);
        unit.setSystemDefined(NOT_SYSTEM_DEFINED);
        return unit;
    }

    private static ProductResponse response(Long id) {
        return new ProductResponse(
                id,
                BOLT_NAME,
                BOLT_ARTICLE,
                UNIT_ID,
                UNIT_NAME,
                NOT_DELETED,
                NOT_SYSTEM_DEFINED
        );
    }
}
