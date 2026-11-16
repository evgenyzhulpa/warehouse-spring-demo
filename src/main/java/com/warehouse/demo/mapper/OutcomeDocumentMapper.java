package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.response.OutcomeDocumentJournalResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentLineResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentResponse;
import com.warehouse.demo.model.OutcomeDocument;
import com.warehouse.demo.model.OutcomeDocumentLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface OutcomeDocumentMapper {

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    OutcomeDocumentJournalResponse toJournalResponse(OutcomeDocument document);

    List<OutcomeDocumentJournalResponse> toJournalResponseList(List<OutcomeDocument> documents);

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    OutcomeDocumentResponse toResponse(OutcomeDocument document);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    OutcomeDocumentLineResponse toLineResponse(OutcomeDocumentLine line);
}
