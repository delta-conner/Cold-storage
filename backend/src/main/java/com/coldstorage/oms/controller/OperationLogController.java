package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String module,
                          @RequestParam(required = false) String username) {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可查看操作日志");
        }
        return Result.ok(operationLogService.page(page, size, module, username));
    }
}
