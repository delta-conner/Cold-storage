package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.WorkOrder;
import com.coldstorage.oms.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String orderNo,
                          @RequestParam(required = false) Long deviceId) {
        return Result.ok(workOrderService.page(page, size, status, orderNo, deviceId));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(workOrderService.detail(id));
    }

    @PostMapping
    public Result<?> submit(@RequestBody WorkOrder form) {
        workOrderService.submit(form);
        return Result.ok();
    }

    @PostMapping("/{id}/assign")
    public Result<?> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        workOrderService.assign(id, body.get("assigneeId"));
        return Result.ok();
    }

    @PostMapping("/{id}/transfer")
    public Result<?> transfer(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        workOrderService.transfer(id, body.get("assigneeId"));
        return Result.ok();
    }

    @PostMapping("/{id}/withdraw")
    public Result<?> withdraw(@PathVariable Long id) {
        workOrderService.withdraw(id);
        return Result.ok();
    }

    @PostMapping("/{id}/start")
    public Result<?> start(@PathVariable Long id) {
        workOrderService.start(id);
        return Result.ok();
    }

    @PostMapping("/{id}/process")
    public Result<?> process(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String record = body.get("processRecord") == null ? null : String.valueOf(body.get("processRecord"));
        boolean finish = body.get("finish") != null && Boolean.parseBoolean(String.valueOf(body.get("finish")));
        String faultType = body.get("faultType") == null ? null : String.valueOf(body.get("faultType"));
        String faultReason = body.get("faultReason") == null ? null : String.valueOf(body.get("faultReason"));
        String solution = body.get("solution") == null ? null : String.valueOf(body.get("solution"));
        String repairImages = body.get("repairImages") == null ? null : String.valueOf(body.get("repairImages"));
        Integer duration = null;
        if (body.get("durationMinutes") != null && StringUtils.hasText(String.valueOf(body.get("durationMinutes")))) {
            duration = Integer.valueOf(String.valueOf(body.get("durationMinutes")));
        }
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> parts =
                body.get("parts") instanceof java.util.List ? (java.util.List<java.util.Map<String, Object>>) body.get("parts") : null;
        workOrderService.process(id, record, finish, faultType, faultReason, solution, duration, repairImages, parts);
        return Result.ok();
    }

    @PostMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String remark = null;
        if (body != null && body.get("acceptRemark") != null) {
            remark = String.valueOf(body.get("acceptRemark"));
        }
        workOrderService.accept(id, remark);
        return Result.ok();
    }

    @PostMapping("/{id}/evaluate")
    public Result<?> evaluate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer satisfaction = body.get("satisfaction") == null ? null : Integer.valueOf(String.valueOf(body.get("satisfaction")));
        String content = body.get("evaluateContent") == null ? null : String.valueOf(body.get("evaluateContent"));
        workOrderService.evaluate(id, satisfaction, content);
        return Result.ok();
    }

    @PostMapping("/{id}/archive")
    public Result<?> archive(@PathVariable Long id) {
        workOrderService.archive(id);
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    public Result<?> close(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String reason = null;
        if (body != null && body.get("closeReason") != null) {
            reason = String.valueOf(body.get("closeReason"));
        }
        workOrderService.close(id, reason);
        return Result.ok();
    }

    @PostMapping("/{id}/reopen")
    public Result<?> reopen(@PathVariable Long id) {
        workOrderService.reopen(id);
        return Result.ok();
    }
}
