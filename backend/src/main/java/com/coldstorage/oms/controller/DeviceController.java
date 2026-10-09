package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.Device;
import com.coldstorage.oms.service.DeviceArchiveService;
import com.coldstorage.oms.service.DeviceService;
import com.coldstorage.oms.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceArchiveService deviceArchiveService;
    @Autowired
    private ExcelService excelService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String deviceNo,
                          @RequestParam(required = false) String deviceName,
                          @RequestParam(required = false) Long roomId,
                          @RequestParam(required = false) String deviceType,
                          @RequestParam(required = false) String status) {
        return Result.ok(deviceService.page(page, size, deviceNo, deviceName, roomId, deviceType, status));
    }

    @GetMapping("/types")
    public Result<?> types() {
        return Result.ok(deviceService.deviceTypes());
    }

    @GetMapping("/repair-options")
    public Result<?> repairOptions() {
        return Result.ok(deviceService.listForRepair());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws Exception {
        byte[] data = excelService.exportDevices();
        String fileName = URLEncoder.encode("设备台账.xlsx", "UTF-8").replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/import")
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(excelService.importDevices(file));
    }

    @GetMapping("/health/summary")
    public Result<?> healthSummary() {
        return Result.ok(deviceArchiveService.healthSummary());
    }

    @GetMapping("/health/page")
    public Result<?> healthPage(@RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestParam(required = false) String healthLevel,
                                @RequestParam(required = false) String deviceName,
                                @RequestParam(required = false) Long roomId) {
        return Result.ok(deviceArchiveService.healthPage(page, size, healthLevel, deviceName, roomId));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(deviceService.detail(id));
    }

    @GetMapping("/{id}/archive")
    public Result<?> archive(@PathVariable Long id) {
        return Result.ok(deviceArchiveService.archive(id));
    }

    @GetMapping("/{id}/status-logs")
    public Result<?> statusLogs(@PathVariable Long id) {
        return Result.ok(deviceService.statusHistory(id));
    }

    @PostMapping("/{id}/status")
    public Result<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        deviceService.changeStatus(id, body.get("status"), body.get("reason"));
        return Result.ok();
    }

    @PostMapping
    public Result<?> save(@RequestBody Device device) {
        deviceService.save(device);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }
}
