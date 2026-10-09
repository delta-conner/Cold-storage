package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.FaultCase;
import com.coldstorage.oms.service.FaultCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fault-cases")
public class FaultCaseController {

    @Autowired
    private FaultCaseService faultCaseService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String deviceType,
                          @RequestParam(required = false) String faultType,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(faultCaseService.page(page, size, deviceType, faultType, keyword));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(faultCaseService.detail(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody FaultCase form) {
        faultCaseService.save(form);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        faultCaseService.delete(id);
        return Result.ok();
    }

    @PostMapping("/promote/{recordId}")
    public Result<?> promote(@PathVariable Long recordId, @RequestBody(required = false) Map<String, String> body) {
        String title = body == null ? null : body.get("title");
        Long id = faultCaseService.promoteFromRecord(recordId, title);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return Result.ok(data);
    }
}
