package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/maintain")
    public Result<?> maintain(@RequestParam(required = false) Integer daysAhead) {
        return Result.ok(statsService.maintainReminders(daysAhead));
    }

    @GetMapping("/admin")
    public Result<?> admin(@RequestParam(required = false) String month) {
        return Result.ok(statsService.adminStats(month));
    }

    @GetMapping("/ops-dashboard")
    public Result<?> opsDashboard() {
        return Result.ok(statsService.opsDashboard());
    }
}
