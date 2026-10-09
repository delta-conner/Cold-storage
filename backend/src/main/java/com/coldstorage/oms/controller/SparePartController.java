package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.SparePart;
import com.coldstorage.oms.service.SparePartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/spare-parts")
public class SparePartController {

    @Autowired
    private SparePartService sparePartService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String partNo,
                          @RequestParam(required = false) String partName,
                          @RequestParam(required = false) String partType,
                          @RequestParam(required = false) Long supplierId,
                          @RequestParam(required = false) Boolean lowOnly) {
        return Result.ok(sparePartService.page(page, size, partNo, partName, partType, supplierId, lowOnly));
    }

    @GetMapping("/options")
    public Result<?> options() {
        return Result.ok(sparePartService.listForSelect());
    }

    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.ok(sparePartService.stockSummary());
    }

    @GetMapping("/records")
    public Result<?> records(@RequestParam(defaultValue = "1") long page,
                             @RequestParam(defaultValue = "10") long size,
                             @RequestParam(required = false) Long partId,
                             @RequestParam(required = false) String changeType) {
        return Result.ok(sparePartService.recordPage(page, size, partId, changeType));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(sparePartService.detail(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody SparePart form) {
        sparePartService.save(form);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sparePartService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/stock")
    public Result<?> stock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String changeType = body.get("changeType") == null ? null : String.valueOf(body.get("changeType"));
        Integer qty = body.get("qty") == null ? null : Integer.valueOf(String.valueOf(body.get("qty")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        sparePartService.changeStock(id, changeType, qty, "MANUAL", null, remark);
        return Result.ok();
    }
}
