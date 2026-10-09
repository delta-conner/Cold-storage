package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.entity.ColdStorage;
import com.coldstorage.oms.entity.Device;
import com.coldstorage.oms.entity.DeviceStatusLog;
import com.coldstorage.oms.mapper.ColdRoomMapper;
import com.coldstorage.oms.mapper.ColdStorageMapper;
import com.coldstorage.oms.mapper.DeviceMapper;
import com.coldstorage.oms.mapper.DeviceStatusLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private static final Set<String> ALL_STATUS = new HashSet<>(Arrays.asList(
            "NORMAL", "FAULT", "REPAIRING", "ACCEPTING", "SCRAPPED"
    ));

    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private ColdStorageMapper coldStorageMapper;
    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;
    @Autowired
    private OperationLogService operationLogService;

    public Page<Device> page(long page, long size, String deviceNo, String deviceName,
                             Long roomId, String deviceType, String status) {
        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deviceNo)) {
            qw.like(Device::getDeviceNo, deviceNo);
        }
        if (StringUtils.hasText(deviceName)) {
            qw.like(Device::getDeviceName, deviceName);
        }
        if (roomId != null) {
            qw.eq(Device::getRoomId, roomId);
        }
        if (StringUtils.hasText(deviceType)) {
            qw.eq(Device::getDeviceType, deviceType);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Device::getStatus, status);
        }
        qw.orderByDesc(Device::getId);
        Page<Device> result = deviceMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        if ("CLIENT".equals(UserContext.getRole())) {
            for (Device d : result.getRecords()) {
                d.setRemark(null);
            }
        }
        return result;
    }

    public Device detail(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        fillExtra(Collections.singletonList(device));
        if ("CLIENT".equals(UserContext.getRole())) {
            device.setRemark(null);
        }
        return device;
    }

    public List<DeviceStatusLog> statusHistory(Long deviceId) {
        detail(deviceId);
        return deviceStatusLogMapper.selectList(new LambdaQueryWrapper<DeviceStatusLog>()
                .eq(DeviceStatusLog::getDeviceId, deviceId)
                .orderByDesc(DeviceStatusLog::getId));
    }

    @Transactional
    public void save(Device device) {
        requireAdminOrOps();
        if (!StringUtils.hasText(device.getDeviceNo()) || !StringUtils.hasText(device.getDeviceName())
                || device.getRoomId() == null) {
            throw new BusinessException("设备编号、名称、冷藏间不能为空");
        }
        if (device.getIsPublic() == null) {
            device.setIsPublic(0);
        }
        if (!StringUtils.hasText(device.getStatus())) {
            device.setStatus("NORMAL");
        }
        if (!ALL_STATUS.contains(device.getStatus())) {
            throw new BusinessException("非法设备状态");
        }
        if (device.getFaultCount() == null) {
            device.setFaultCount(0);
        }
        Device exists = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceNo, device.getDeviceNo())
                .ne(device.getId() != null, Device::getId, device.getId()));
        if (exists != null) {
            throw new BusinessException("设备编号已存在");
        }
        if (device.getId() == null) {
            deviceMapper.insert(device);
            writeLog(device.getId(), null, device.getStatus(), "新建设备", "MANUAL");
            operationLogService.log("设备管理", "新增", device.getDeviceNo());
        } else {
            Device db = deviceMapper.selectById(device.getId());
            if (db == null) {
                throw new BusinessException("设备不存在");
            }
            String oldStatus = db.getStatus() == null ? "NORMAL" : db.getStatus();
            String newStatus = device.getStatus();
            // 编辑表单改状态走校验
            if (!oldStatus.equals(newStatus)) {
                assertManualTransition(oldStatus, newStatus);
            }
            deviceMapper.updateById(device);
            if (!oldStatus.equals(newStatus)) {
                writeLog(device.getId(), oldStatus, newStatus, "台账编辑变更状态", "MANUAL");
            }
            operationLogService.log("设备管理", "编辑", device.getDeviceNo());
        }
    }

    /**
     * 手工变更状态（详情页：报废 / 恢复 / 纠偏）
     */
    @Transactional
    public void changeStatus(Long id, String toStatus, String reason) {
        requireAdminOrOps();
        if (!StringUtils.hasText(toStatus) || !ALL_STATUS.contains(toStatus)) {
            throw new BusinessException("目标状态非法");
        }
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        String from = device.getStatus() == null ? "NORMAL" : device.getStatus();
        if (from.equals(toStatus)) {
            throw new BusinessException("状态未变化");
        }
        if ("SCRAPPED".equals(toStatus) || "NORMAL".equals(toStatus) && "SCRAPPED".equals(from)) {
            if (!"ADMIN".equals(UserContext.getRole())) {
                throw new BusinessException("仅管理员可报废或恢复设备");
            }
        }
        assertManualTransition(from, toStatus);
        device.setStatus(toStatus);
        deviceMapper.updateById(device);
        writeLog(id, from, toStatus,
                StringUtils.hasText(reason) ? reason : "手工变更", "MANUAL");
        operationLogService.log("设备生命周期", "状态变更",
                device.getDeviceNo() + " " + from + "→" + toStatus);
    }

    /**
     * 工单等业务节点联动（已报废设备不改；状态相同不写日志）
     */
    @Transactional
    public void applyStatusFromBiz(Long deviceId, String toStatus, String reason) {
        if (deviceId == null || !StringUtils.hasText(toStatus)) {
            return;
        }
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return;
        }
        String from = device.getStatus() == null ? "NORMAL" : device.getStatus();
        if ("SCRAPPED".equals(from)) {
            return;
        }
        if (from.equals(toStatus)) {
            return;
        }
        device.setStatus(toStatus);
        deviceMapper.updateById(device);
        writeLog(deviceId, from, toStatus,
                StringUtils.hasText(reason) ? reason : "业务联动", "WORK_ORDER");
    }

    public void delete(Long id) {
        requireAdminOrOps();
        deviceMapper.deleteById(id);
        operationLogService.log("设备管理", "删除", "设备ID：" + id);
    }

    public List<Device> listForRepair() {
        List<Device> list = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .ne(Device::getStatus, "SCRAPPED")
                .orderByAsc(Device::getId));
        fillExtra(list);
        return list;
    }

    public List<String> deviceTypes() {
        return Arrays.asList(
                "约克主机", "比泽尔主机", "复叠活塞机",
                "冷藏间蒸发器/风机", "融霜相关设备", "排水电热", "传感器/保护类"
        );
    }

    private void assertManualTransition(String from, String to) {
        Map<String, Set<String>> allow = new HashMap<>();
        allow.put("NORMAL", setOf("FAULT", "SCRAPPED"));
        allow.put("FAULT", setOf("NORMAL", "REPAIRING", "SCRAPPED"));
        allow.put("REPAIRING", setOf("ACCEPTING", "FAULT", "SCRAPPED"));
        allow.put("ACCEPTING", setOf("NORMAL", "REPAIRING", "SCRAPPED"));
        allow.put("SCRAPPED", setOf("NORMAL"));
        Set<String> next = allow.getOrDefault(from, Collections.emptySet());
        if (!next.contains(to)) {
            throw new BusinessException("不允许从「" + label(from) + "」变更为「" + label(to) + "」");
        }
    }

    private Set<String> setOf(String... arr) {
        return new HashSet<>(Arrays.asList(arr));
    }

    private String label(String s) {
        Map<String, String> m = new HashMap<>();
        m.put("NORMAL", "正常");
        m.put("FAULT", "故障");
        m.put("REPAIRING", "维修中");
        m.put("ACCEPTING", "待验收");
        m.put("SCRAPPED", "报废");
        return m.getOrDefault(s, s);
    }

    private void writeLog(Long deviceId, String from, String to, String reason, String source) {
        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(deviceId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setReason(reason);
        log.setSource(source);
        try {
            log.setOperatorId(UserContext.getUserId());
            log.setOperatorName(UserContext.getUsername());
        } catch (Exception ignored) {
            log.setOperatorName("系统");
        }
        deviceStatusLogMapper.insert(log);
    }

    private void fillExtra(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        Map<Long, String> storageMap = coldStorageMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdStorage::getId, ColdStorage::getName, (a, b) -> a));
        for (Device d : devices) {
            ColdRoom room = roomMap.get(d.getRoomId());
            if (room != null) {
                d.setRoomName(room.getName());
                d.setStorageId(room.getStorageId());
                d.setStorageName(storageMap.get(room.getStorageId()));
            }
        }
    }

    private void requireAdminOrOps() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限");
        }
    }
}
