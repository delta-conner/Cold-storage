package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.JwtUtil;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.dto.LoginRequest;
import com.coldstorage.oms.dto.LoginResponse;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.entity.UserColdRoom;
import com.coldstorage.oms.mapper.SysUserMapper;
import com.coldstorage.oms.mapper.UserColdRoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private UserColdRoomMapper userColdRoomMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private AppCacheService appCacheService;

    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已禁用，请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        resp.setPhone(user.getPhone());
        if ("CLIENT".equals(user.getRole())) {
            List<Long> roomIds = userColdRoomMapper.selectList(new LambdaQueryWrapper<UserColdRoom>()
                            .eq(UserColdRoom::getUserId, user.getId()))
                    .stream().map(UserColdRoom::getRoomId).collect(Collectors.toList());
            resp.setRoomIds(roomIds);
        }
        // Redis 会话标记（降级也不影响登录）
        appCacheService.set("session:" + user.getId(), token, 24 * 3600L);
        operationLogService.log(user.getId(), user.getUsername(), user.getRole(), "认证", "登录", "用户登录成功");
        return resp;
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            appCacheService.set("token:blacklist:" + token, "1", 24 * 3600L);
        }
        try {
            operationLogService.log("认证", "退出", "用户退出登录");
        } catch (Exception ignored) {
            // ignore
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return appCacheService.has("token:blacklist:" + token);
    }

    public SysUser profile() {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public void updateProfile(SysUser form) {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRealName(form.getRealName());
        user.setPhone(form.getPhone());
        if (StringUtils.hasText(form.getPassword())) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        userMapper.updateById(user);
    }
}
