package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.MaintainPlan;
import com.coldstorage.oms.service.MaintainPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/maintain-plans")
public class MaintainPlanController {

    @Autowired
    private MaintainPlanService maintainPlanService;

    @GetMapping("/default-items")
    public Result<?> defaultItems(@RequestParam(required = false) String deviceType) {
        return Result.ok(maintainPlanService.defaultItems(deviceType));
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String cycleType) {
        return Result.ok(maintainPlanService.page(page, size, status, cycleType));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(maintainPlanService.detail(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody MaintainPlan form) {
        maintainPlanService.save(form);
        return Result.ok();
    }

    @PostMapping("/{id}/generate-tasks")
    public Result<?> generate(@PathVariable Long id) {
        int created = maintainPlanService.generateTasks(id);
        Map<String, Object> data = new HashMap<>();
        data.put("created", created);
        return Result.ok(data);
    }

    @PostMapping("/{id}/close")
    public Result<?> close(@PathVariable Long id) {
        maintainPlanService.close(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        maintainPlanService.delete(id);
        return Result.ok();
    }
}
