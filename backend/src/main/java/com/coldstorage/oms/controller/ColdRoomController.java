package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.service.ColdRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class ColdRoomController {

    @Autowired
    private ColdRoomService coldRoomService;

    @GetMapping("/storages")
    public Result<?> storages() {
        return Result.ok(coldRoomService.listStorages());
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Long storageId,
                          @RequestParam(required = false) String name) {
        return Result.ok(coldRoomService.pageRooms(page, size, storageId, name));
    }

    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(coldRoomService.listForSelect());
    }

    @PostMapping
    public Result<?> save(@RequestBody ColdRoom room) {
        coldRoomService.save(room);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        coldRoomService.delete(id);
        return Result.ok();
    }
}
