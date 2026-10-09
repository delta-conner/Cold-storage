package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.OperationLog;
import com.coldstorage.oms.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    public void log(String module, String action, String detail) {
        log(UserContext.getUserId(), UserContext.getUsername(), UserContext.getRole(), module, action, detail);
    }

    public void log(Long userId, String username, String role, String module, String action, String detail) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setRole(role);
            log.setModule(module);
            log.setAction(action);
            log.setDetail(detail);
            log.setIp(resolveIp());
            operationLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不影响主业务
        }
    }

    public Page<OperationLog> page(long page, long size, String module, String username) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            qw.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(username)) {
            qw.like(OperationLog::getUsername, username);
        }
        qw.orderByDesc(OperationLog::getId);
        return operationLogMapper.selectPage(new Page<>(page, size), qw);
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            } else {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
