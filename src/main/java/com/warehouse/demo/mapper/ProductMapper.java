package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.request.CreateProductRequest;
import com.warehouse.demo.dto.request.UpdateProductRequest;
import com.warehouse.demo.dto.response.ProductResponse;
import com.warehouse.demo.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "unit", ignore = true)
    Product createProductRequestToProduct(CreateProductRequest request);

    @Mapping(target = "unit", ignore = true)
    void updateProductFromUpdateProductRequest(UpdateProductRequest request, @MappingTarget Product product);

    @Mapping(source = "unit.id", target = "unitId")
    @Mapping(source = "unit.name", target = "unitName")
    ProductResponse productToProductResponse(Product product);

    List<ProductResponse> productListToProductResponseList(List<Product> productList);
}
