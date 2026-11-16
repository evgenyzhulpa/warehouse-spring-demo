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
import com.warehouse.demo.service.ProductService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;

    @Transactional(readOnly = true)
    @Override
    public ProductListResponse findAll(String name, String article, Pageable pageable) {
        Page<Product> page = productRepository.findAll(productFilter(name, article), pageable);
        return new ProductListResponse(
                productMapper.productListToProductResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Specification<Product> productFilter(String name, String article) {
        return (root, query, cb) -> {
            if (query != null && Product.class.equals(query.getResultType())) {
                root.fetch("unit", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), contains(name)));
            }
            if (StringUtils.hasText(article)) {
                predicates.add(cb.like(cb.lower(root.get("article")), contains(article)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponse findById(Long id) {
        return productMapper.productToProductResponse(findProductById(id));
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Номенклатура с ID {0} не найдена!", id)
                ));
    }

    private Unit findUnitById(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Единица измерения с ID {0} не найдена!", unitId)
                ));
    }

    @Transactional
    @Override
    public ProductResponse save(CreateProductRequest createProductRequest) {
        Product product = productMapper.createProductRequestToProduct(createProductRequest);
        product.setUnit(findUnitById(createProductRequest.unitId()));
        return productMapper.productToProductResponse(productRepository.save(product));
    }

    @Transactional
    @Override
    public ProductResponse update(Long id, UpdateProductRequest updateProductRequest) {
        Product existingProduct = findProductById(id);
        if (existingProduct.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенной номенклатуры запрещено");
        }
        productMapper.updateProductFromUpdateProductRequest(updateProductRequest, existingProduct);
        existingProduct.setUnit(findUnitById(updateProductRequest.unitId()));
        return productMapper.productToProductResponse(productRepository.save(existingProduct));
    }

    @Transactional
    @Override
    public ProductResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    private ProductResponse setDeletedValue(Long id, boolean isDeleted) {
        Product existingProduct = findProductById(id);
        if (isDeleted && existingProduct.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенной номенклатуры запрещено");
        }
        existingProduct.setDeleted(isDeleted);
        return productMapper.productToProductResponse(productRepository.save(existingProduct));
    }

    @Transactional
    @Override
    public ProductResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Product existingProduct = findProductById(id);
        if (existingProduct.getSystemDefined()) {
            throw new BusinessLogicException("Удаление предопределенной номенклатуры запрещено");
        }
        productRepository.delete(existingProduct);
    }
}
