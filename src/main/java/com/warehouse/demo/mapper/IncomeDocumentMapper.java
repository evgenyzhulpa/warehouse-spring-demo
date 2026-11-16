package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.response.IncomeDocumentJournalResponse;
import com.warehouse.demo.dto.response.IncomeDocumentLineResponse;
import com.warehouse.demo.dto.response.IncomeDocumentResponse;
import com.warehouse.demo.model.IncomeDocument;
import com.warehouse.demo.model.IncomeDocumentLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IncomeDocumentMapper {

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    IncomeDocumentJournalResponse toJournalResponse(IncomeDocument document);

    List<IncomeDocumentJournalResponse> toJournalResponseList(List<IncomeDocument> documents);

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    IncomeDocumentResponse toResponse(IncomeDocument document);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    IncomeDocumentLineResponse toLineResponse(IncomeDocumentLine line);
}
