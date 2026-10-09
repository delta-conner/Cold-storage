package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer isRead) {
        return Result.ok(messageService.page(page, size, isRead));
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        Map<String, Object> map = new HashMap<>();
        map.put("count", messageService.unreadCount());
        return Result.ok(map);
    }

    @PostMapping("/{id}/read")
    public Result<?> read(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<?> readAll() {
        messageService.markAllRead();
        return Result.ok();
    }
}
