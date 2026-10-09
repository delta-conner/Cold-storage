package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.Supplier;
import com.coldstorage.oms.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String status) {
        return Result.ok(supplierService.page(page, size, name, status));
    }

    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(supplierService.listActive());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(supplierService.detail(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody Supplier form) {
        supplierService.save(form);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.ok();
    }
}
