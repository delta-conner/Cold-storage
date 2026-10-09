package com.coldstorage.oms.controller;

import com.coldstorage.oms.common.Result;
import com.coldstorage.oms.dto.LoginRequest;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/profile")
    public Result<?> profile() {
        return Result.ok(authService.profile());
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody SysUser form) {
        authService.updateProfile(form);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        authService.logout(auth);
        return Result.ok();
    }
}
