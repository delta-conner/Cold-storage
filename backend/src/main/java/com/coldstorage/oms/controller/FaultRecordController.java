package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.FaultRecord;
import com.coldstorage.oms.service.FaultRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faults")
public class FaultRecordController {

    @Autowired
    private FaultRecordService faultRecordService;

    @GetMapping("/types")
    public Result<?> types() {
        return Result.ok(faultRecordService.faultTypes());
    }

    @GetMapping("/levels")
    public Result<?> levels() {
        return Result.ok(faultRecordService.faultLevels());
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Long deviceId,
                          @RequestParam(required = false) String faultType,
                          @RequestParam(required = false) String faultLevel,
                          @RequestParam(required = false) String startTime,
                          @RequestParam(required = false) String endTime) {
        return Result.ok(faultRecordService.page(page, size, deviceId, faultType, faultLevel, startTime, endTime));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(faultRecordService.detail(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody FaultRecord form) {
        faultRecordService.save(form);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        faultRecordService.delete(id);
        return Result.ok();
    }
}
