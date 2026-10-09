package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.entity.ColdStorage;
import com.coldstorage.oms.entity.UserColdRoom;
import com.coldstorage.oms.mapper.ColdRoomMapper;
import com.coldstorage.oms.mapper.ColdStorageMapper;
import com.coldstorage.oms.mapper.UserColdRoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ColdRoomService {

    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private ColdStorageMapper coldStorageMapper;
    @Autowired
    private UserColdRoomMapper userColdRoomMapper;

    public List<ColdStorage> listStorages() {
        return coldStorageMapper.selectList(new LambdaQueryWrapper<ColdStorage>().orderByAsc(ColdStorage::getId));
    }

    public Page<ColdRoom> pageRooms(long page, long size, Long storageId, String name) {
        LambdaQueryWrapper<ColdRoom> qw = new LambdaQueryWrapper<>();
        if (storageId != null) {
            qw.eq(ColdRoom::getStorageId, storageId);
        }
        if (StringUtils.hasText(name)) {
            qw.like(ColdRoom::getName, name);
        }
        // 甲方可查看全部冷藏间；绑定仅用于个人中心展示
        qw.orderByAsc(ColdRoom::getId);
        Page<ColdRoom> result = coldRoomMapper.selectPage(new Page<>(page, size), qw);
        fillStorageName(result.getRecords());
        return result;
    }

    public List<ColdRoom> listForSelect() {
        List<ColdRoom> list = coldRoomMapper.selectList(new LambdaQueryWrapper<ColdRoom>().orderByAsc(ColdRoom::getId));
        fillStorageName(list);
        return list;
    }

    public void save(ColdRoom room) {
        requireAdmin();
        if (room.getStorageId() == null || !StringUtils.hasText(room.getName())) {
            throw new BusinessException("冷库和冷藏间名称不能为空");
        }
        if (!StringUtils.hasText(room.getStatus())) {
            room.setStatus("ENABLED");
        }
        if (room.getId() == null) {
            coldRoomMapper.insert(room);
        } else {
            coldRoomMapper.updateById(room);
        }
    }

    public void delete(Long id) {
        requireAdmin();
        coldRoomMapper.deleteById(id);
        userColdRoomMapper.delete(new LambdaQueryWrapper<UserColdRoom>().eq(UserColdRoom::getRoomId, id));
    }

    public List<Long> clientRoomIds() {
        return userColdRoomMapper.selectList(new LambdaQueryWrapper<UserColdRoom>()
                        .eq(UserColdRoom::getUserId, UserContext.getUserId()))
                .stream().map(UserColdRoom::getRoomId).collect(Collectors.toList());
    }

    private void fillStorageName(List<ColdRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        Map<Long, String> map = coldStorageMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdStorage::getId, ColdStorage::getName, (a, b) -> a));
        for (ColdRoom room : rooms) {
            room.setStorageName(map.get(room.getStorageId()));
        }
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("无权限");
        }
    }
}
