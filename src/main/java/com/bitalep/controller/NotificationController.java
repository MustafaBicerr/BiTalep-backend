package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.MiscDtos;
import com.bitalep.service.impl.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiSuccessResponse<List<MiscDtos.NotificationResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        var result = notificationService.list(page, pageSize);
        return ApiSuccessResponse.page(result.data(), result.meta());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ApiSuccessResponse<MiscDtos.UnreadCount> unread() {
        return ApiSuccessResponse.of(notificationService.unreadCount());
    }
}
