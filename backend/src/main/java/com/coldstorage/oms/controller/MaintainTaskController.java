package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.MaintainTask;
import com.coldstorage.oms.service.MaintainTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/maintain-tasks")
public class MaintainTaskController {

    @Autowired
    private MaintainTaskService maintainTaskService;

    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.ok(maintainTaskService.summary());
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Long planId,
                          @RequestParam(required = false) String displayFilter) {
        return Result.ok(maintainTaskService.page(page, size, status, planId, displayFilter));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(maintainTaskService.detail(id));
    }

    @PostMapping("/{id}/assign")
    public Result<?> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        maintainTaskService.assign(id, body.get("assigneeId"));
        return Result.ok();
    }

    @PostMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id, @RequestBody MaintainTask form) {
        maintainTaskService.complete(id, form);
        return Result.ok();
    }
}
