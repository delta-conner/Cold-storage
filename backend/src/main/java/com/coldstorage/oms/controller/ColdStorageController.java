package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.ColdStorage;
import com.coldstorage.oms.service.ColdStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storages")
public class ColdStorageController {

    @Autowired
    private ColdStorageService coldStorageService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String status) {
        return Result.ok(coldStorageService.page(page, size, name, status));
    }

    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(coldStorageService.listAll());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(coldStorageService.detail(id));
    }

    @GetMapping("/{id}/overview")
    public Result<?> overview(@PathVariable Long id) {
        return Result.ok(coldStorageService.overview(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody ColdStorage form) {
        coldStorageService.save(form);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        coldStorageService.delete(id);
        return Result.ok();
    }
}
