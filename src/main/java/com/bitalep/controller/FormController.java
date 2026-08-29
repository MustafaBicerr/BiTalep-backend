package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.FileDtos;
import com.bitalep.dto.FormDtos;
import com.bitalep.entity.Department;
import com.bitalep.entity.FormType;
import com.bitalep.entity.RequestStatus;
import com.bitalep.service.impl.FormService;
import com.bitalep.util.InstantQuery;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @GetMapping
    public ApiSuccessResponse<List<FormDtos.ApplicationResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) List<RequestStatus> status,
            @RequestParam(required = false) List<FormType> formType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID applicantId,
            @RequestParam(required = false) List<Department> department,
            @RequestParam(required = false) Boolean hasAttachments,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedBefore
    ) {
        var result = formService.list(
                page, pageSize, sortBy, sortOrder, status, formType,
                InstantQuery.start(dateFrom), InstantQuery.endInclusive(dateTo),
                keyword, applicantId, department, hasAttachments, updatedBefore);
        return ApiSuccessResponse.page(result.data(), result.meta());
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<FormDtos.ApplicationResponse> get(@PathVariable UUID id) {
        return ApiSuccessResponse.of(formService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<FormDtos.ApplicationResponse>> create(
            @Valid @RequestBody FormDtos.CreateApplicationRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(formService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiSuccessResponse<FormDtos.ApplicationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody FormDtos.UpdateApplicationRequest req
    ) {
        return ApiSuccessResponse.of(formService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        formService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/review")
    public ApiSuccessResponse<FormDtos.ApplicationResponse> review(@PathVariable UUID id) {
        return ApiSuccessResponse.of(formService.review(id));
    }

    @PutMapping("/{id}/approve")
    public ApiSuccessResponse<FormDtos.ApplicationResponse> approve(@PathVariable UUID id) {
        return ApiSuccessResponse.of(formService.approve(id));
    }

    @PutMapping("/{id}/reject")
    public ApiSuccessResponse<FormDtos.ApplicationResponse> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) FormDtos.RejectRequest body
    ) {
        String reason = body == null ? null : body.reason();
        return ApiSuccessResponse.of(formService.reject(id, reason));
    }

    @PutMapping("/{id}/needs-update")
    public ApiSuccessResponse<FormDtos.ApplicationResponse> needsUpdate(
            @PathVariable UUID id,
            @RequestBody(required = false) FormDtos.NeedsUpdateRequest body
    ) {
        String reason = body == null ? null : body.reason();
        return ApiSuccessResponse.of(formService.needsUpdate(id, reason));
    }

    @GetMapping("/{id}/files")
    public ApiSuccessResponse<List<FileDtos.AttachmentResponse>> files(@PathVariable UUID id) {
        return ApiSuccessResponse.of(formService.files(id));
    }
}
