package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.FileDtos;
import com.bitalep.service.impl.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiSuccessResponse<FileDtos.AttachmentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("applicationId") UUID applicationId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(fileService.upload(file, applicationId)));
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<FileDtos.AttachmentResponse> get(@PathVariable UUID id) {
        return ApiSuccessResponse.of(fileService.get(id));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<?> content(@PathVariable UUID id) {
        FileService.FileContent c = fileService.content(id);
        return ResponseEntity.ok()
                .contentType(fileService.mediaType(c.mime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                        new String(c.downloadName().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"")
                .body(c.resource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ApiSuccessResponse<List<FileDtos.AttachmentResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        var result = fileService.listAll(page, pageSize);
        return ApiSuccessResponse.page(result.data(), result.meta());
    }
}
