package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.entity.UserColdRoom;
import com.coldstorage.oms.entity.WorkOrder;
import com.coldstorage.oms.mapper.ColdRoomMapper;
import com.coldstorage.oms.mapper.SysUserMapper;
import com.coldstorage.oms.mapper.UserColdRoomMapper;
import com.coldstorage.oms.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private UserColdRoomMapper userColdRoomMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public Page<SysUser> page(long page, long size, String username, String role) {
        requireAdmin();
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            qw.like(SysUser::getUsername, username);
        }
        if (StringUtils.hasText(role)) {
            qw.eq(SysUser::getRole, role);
        }
        qw.orderByAsc(SysUser::getId);
        return userMapper.selectPage(new Page<>(page, size), qw);
    }

    public List<SysUser> listOps() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "OPS")
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
    }

    public Map<String, Object> detail(Long id) {
        requireAdmin();
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<Long> roomIds = userColdRoomMapper.selectList(new LambdaQueryWrapper<UserColdRoom>()
                        .eq(UserColdRoom::getUserId, id))
                .stream().map(UserColdRoom::getRoomId).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("roomIds", roomIds);
        return map;
    }

    @Transactional
    public void save(SysUser user, List<Long> roomIds) {
        requireAdmin();
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getRole())) {
            throw new BusinessException("用户名和角色不能为空");
        }
        if (user.getId() == null) {
            if (!StringUtils.hasText(user.getPassword())) {
                user.setPassword("123456");
            }
            Long cnt = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, user.getUsername()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("用户名已存在");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            if (user.getStatus() == null) {
                user.setStatus(1);
            }
            userMapper.insert(user);
        } else {
            SysUser db = userMapper.selectById(user.getId());
            if (db == null) {
                throw new BusinessException("用户不存在");
            }
            db.setRealName(user.getRealName());
            db.setPhone(user.getPhone());
            db.setRole(user.getRole());
            if (user.getStatus() != null) {
                db.setStatus(user.getStatus());
            }
            if (StringUtils.hasText(user.getPassword())) {
                db.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            userMapper.updateById(db);
            user.setId(db.getId());
        }
        userColdRoomMapper.delete(new LambdaQueryWrapper<UserColdRoom>().eq(UserColdRoom::getUserId, user.getId()));
        if ("CLIENT".equals(user.getRole()) && roomIds != null) {
            for (Long roomId : roomIds) {
                UserColdRoom rel = new UserColdRoom();
                rel.setUserId(user.getId());
                rel.setRoomId(roomId);
                userColdRoomMapper.insert(rel);
            }
        }
    }

    public void updateStatus(Long id, Integer status) {
        requireAdmin();
        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException("不能禁用当前登录用户");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status == null ? 1 : status);
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        requireAdmin();
        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException("不能删除当前登录用户");
        }
        userMapper.deleteById(id);
        userColdRoomMapper.delete(new LambdaQueryWrapper<UserColdRoom>().eq(UserColdRoom::getUserId, id));
    }

    public Map<String, Object> clientSummary() {
        if (!"CLIENT".equals(UserContext.getRole())) {
            throw new BusinessException("仅甲方可查看");
        }
        Long uid = UserContext.getUserId();
        List<Long> roomIds = userColdRoomMapper.selectList(new LambdaQueryWrapper<UserColdRoom>()
                        .eq(UserColdRoom::getUserId, uid))
                .stream().map(UserColdRoom::getRoomId).collect(Collectors.toList());
        List<Map<String, Object>> rooms = new ArrayList<>();
        if (!roomIds.isEmpty()) {
            List<ColdRoom> roomList = coldRoomMapper.selectBatchIds(roomIds);
            for (ColdRoom r : roomList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.getId());
                item.put("name", r.getName());
                item.put("code", r.getCode());
                rooms.add(item);
            }
        }
        List<WorkOrder> orders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getSubmitUserId, uid));
        long total = orders.size();
        long pending = orders.stream().filter(o -> "PENDING".equals(o.getStatus()) || "ASSIGNED".equals(o.getStatus())).count();
        long processing = orders.stream().filter(o -> "PROCESSING".equals(o.getStatus()) || "ACCEPTING".equals(o.getStatus())).count();
        long done = orders.stream().filter(o -> "DONE".equals(o.getStatus()) || "EVALUATED".equals(o.getStatus()) || "ARCHIVED".equals(o.getStatus())).count();
        Map<String, Object> map = new HashMap<>();
        map.put("roomIds", roomIds);
        map.put("rooms", rooms);
        map.put("orderTotal", total);
        map.put("orderPending", pending);
        map.put("orderProcessing", processing);
        map.put("orderDone", done);
        map.put("completionRate", total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        return map;
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("无权限");
        }
    }
}
