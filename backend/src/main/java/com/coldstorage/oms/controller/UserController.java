package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String role) {
        return Result.ok(userService.page(page, size, username, role));
    }

    @GetMapping("/ops")
    public Result<?> ops() {
        return Result.ok(userService.listOps());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody Map<String, Object> body) {
        SysUser user = new SysUser();
        if (body.get("id") != null) {
            user.setId(Long.valueOf(String.valueOf(body.get("id"))));
        }
        user.setUsername((String) body.get("username"));
        user.setPassword((String) body.get("password"));
        user.setRealName((String) body.get("realName"));
        user.setPhone((String) body.get("phone"));
        user.setRole((String) body.get("role"));
        if (body.get("status") != null) {
            user.setStatus(Integer.valueOf(String.valueOf(body.get("status"))));
        }
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("roomIds");
        List<Long> roomIds = null;
        if (raw != null) {
            roomIds = raw.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(java.util.stream.Collectors.toList());
        }
        userService.save(user, roomIds);
        return Result.ok();
    }

    @PostMapping("/{id}/status")
    public Result<?> status(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status")));
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @GetMapping("/client-summary")
    public Result<?> clientSummary() {
        return Result.ok(userService.clientSummary());
    }
}
