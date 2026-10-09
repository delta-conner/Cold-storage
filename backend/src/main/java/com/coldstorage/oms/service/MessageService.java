package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.SysMessage;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.mapper.SysMessageMapper;
import com.coldstorage.oms.mapper.SysUserMapper;
import com.coldstorage.oms.websocket.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private SysMessageMapper messageMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private MessageWebSocketHandler webSocketHandler;

    public Page<SysMessage> page(long page, long size, Integer isRead) {
        Long uid = UserContext.getUserId();
        LambdaQueryWrapper<SysMessage> qw = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, uid);
        if (isRead != null) {
            qw.eq(SysMessage::getIsRead, isRead);
        }
        qw.orderByDesc(SysMessage::getId);
        return messageMapper.selectPage(new Page<>(page, size), qw);
    }

    public long unreadCount() {
        Long cnt = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, UserContext.getUserId())
                .eq(SysMessage::getIsRead, 0));
        return cnt == null ? 0 : cnt;
    }

    public void markRead(Long id) {
        SysMessage msg = messageMapper.selectById(id);
        if (msg == null || !UserContext.getUserId().equals(msg.getUserId())) {
            throw new BusinessException("消息不存在");
        }
        msg.setIsRead(1);
        messageMapper.updateById(msg);
    }

    public void markAllRead() {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getUserId, UserContext.getUserId())
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1));
    }

    public void sendToUser(Long userId, String title, String content, String msgType, String bizType, Long bizId) {
        if (userId == null) {
            return;
        }
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMsgType(msgType);
        msg.setBizType(bizType);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        messageMapper.insert(msg);
        Map<String, Object> push = new HashMap<>();
        push.put("type", "MESSAGE");
        push.put("id", msg.getId());
        push.put("title", title);
        push.put("content", content);
        push.put("msgType", msgType);
        push.put("unread", true);
        webSocketHandler.pushToUser(userId, push);
    }

    public void sendToRole(String role, String title, String content, String msgType, String bizType, Long bizId) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, role)
                .eq(SysUser::getStatus, 1));
        for (SysUser u : users) {
            sendToUser(u.getId(), title, content, msgType, bizType, bizId);
        }
    }

    public void sendToAdmins(String title, String content, String msgType, String bizType, Long bizId) {
        sendToRole("ADMIN", title, content, msgType, bizType, bizId);
    }
}
