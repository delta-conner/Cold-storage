package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.entity.ColdStorage;
import com.coldstorage.oms.entity.Device;
import com.coldstorage.oms.entity.WorkOrder;
import com.coldstorage.oms.mapper.ColdRoomMapper;
import com.coldstorage.oms.mapper.ColdStorageMapper;
import com.coldstorage.oms.mapper.DeviceMapper;
import com.coldstorage.oms.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ColdStorageService {

    @Autowired
    private ColdStorageMapper coldStorageMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private OperationLogService operationLogService;

    public Page<ColdStorage> page(long page, long size, String name, String status) {
        LambdaQueryWrapper<ColdStorage> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            qw.like(ColdStorage::getName, name);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(ColdStorage::getStatus, status);
        }
        qw.orderByAsc(ColdStorage::getId);
        Page<ColdStorage> result = coldStorageMapper.selectPage(new Page<>(page, size), qw);
        for (ColdStorage s : result.getRecords()) {
            fillCount(s);
        }
        return result;
    }

    public List<ColdStorage> listAll() {
        return coldStorageMapper.selectList(new LambdaQueryWrapper<ColdStorage>().orderByAsc(ColdStorage::getId));
    }

    public ColdStorage detail(Long id) {
        ColdStorage s = coldStorageMapper.selectById(id);
        if (s == null) {
            throw new BusinessException("冷库不存在");
        }
        fillCount(s);
        return s;
    }

    /**
     * 库区办事台：档案 + 冷藏间清单 + 待办（未完成工单 / 异常设备 / 临近维保）。
     * 不做完成率、趋势、类型分布等分析指标（那些归数据统计中心）。
     */
    public Map<String, Object> overview(Long id) {
        ColdStorage storage = detail(id);
        List<ColdRoom> rooms = coldRoomMapper.selectList(new LambdaQueryWrapper<ColdRoom>()
                .eq(ColdRoom::getStorageId, id)
                .orderByAsc(ColdRoom::getId));
        List<Long> roomIds = rooms.stream().map(ColdRoom::getId).collect(Collectors.toList());

        List<Map<String, Object>> openOrders = new ArrayList<>();
        List<Map<String, Object>> maintainList = new ArrayList<>();
        List<Map<String, Object>> attentionDevices = new ArrayList<>();

        if (!roomIds.isEmpty()) {
            List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                    .in(Device::getRoomId, roomIds)
                    .orderByAsc(Device::getId));
            Map<Long, Device> deviceMap = devices.stream()
                    .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
            Map<Long, String> roomNameMap = rooms.stream()
                    .collect(Collectors.toMap(ColdRoom::getId, ColdRoom::getName, (a, b) -> a));
            List<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());

            for (Device d : devices) {
                String st = d.getStatus() == null ? "NORMAL" : d.getStatus();
                if ("FAULT".equals(st) || "REPAIRING".equals(st) || "ACCEPTING".equals(st)) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("deviceId", d.getId());
                    row.put("deviceName", d.getDeviceName());
                    row.put("deviceNo", d.getDeviceNo());
                    row.put("status", st);
                    row.put("roomName", roomNameMap.get(d.getRoomId()));
                    attentionDevices.add(row);
                }
            }

            if (!deviceIds.isEmpty()) {
                List<WorkOrder> orders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                        .in(WorkOrder::getDeviceId, deviceIds)
                        .notIn(WorkOrder::getStatus, Arrays.asList("DONE", "CLOSED", "ARCHIVED", "EVALUATED"))
                        .orderByDesc(WorkOrder::getId)
                        .last("LIMIT 15"));
                for (WorkOrder o : orders) {
                    Device d = deviceMap.get(o.getDeviceId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", o.getId());
                    row.put("orderNo", o.getOrderNo());
                    row.put("status", o.getStatus());
                    row.put("faultDesc", o.getFaultDesc());
                    row.put("createTime", o.getCreateTime());
                    row.put("deviceName", d == null ? null : d.getDeviceName());
                    row.put("roomName", d == null ? null : roomNameMap.get(d.getRoomId()));
                    openOrders.add(row);
                }
            }

            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for (Device d : devices) {
                if (d.getInstallDate() == null || d.getMaintainCycleDays() == null || d.getMaintainCycleDays() <= 0) {
                    continue;
                }
                int cycle = d.getMaintainCycleDays();
                Date next = d.getInstallDate();
                while (next.before(now)) {
                    cal.setTime(next);
                    cal.add(Calendar.DAY_OF_MONTH, cycle);
                    next = cal.getTime();
                }
                long diff = (next.getTime() - now.getTime()) / (24L * 3600 * 1000);
                if (diff <= 30) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("deviceId", d.getId());
                    row.put("deviceName", d.getDeviceName());
                    row.put("deviceNo", d.getDeviceNo());
                    row.put("roomName", roomNameMap.get(d.getRoomId()));
                    row.put("nextMaintainDate", sdf.format(next));
                    row.put("daysLeft", diff);
                    row.put("urgent", diff <= 7);
                    maintainList.add(row);
                }
            }
            maintainList.sort(Comparator.comparingLong(m -> ((Number) m.get("daysLeft")).longValue()));
            if (maintainList.size() > 15) {
                maintainList = maintainList.subList(0, 15);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scope", "STORAGE_ACTION");
        result.put("scopeLabel", "库区办事台");
        result.put("storage", storage);
        result.put("rooms", rooms);
        result.put("openOrders", openOrders);
        result.put("maintainList", maintainList);
        result.put("attentionDevices", attentionDevices);
        return result;
    }

    public void save(ColdStorage form) {
        requireAdmin();
        if (!StringUtils.hasText(form.getName())) {
            throw new BusinessException("冷库名称不能为空");
        }
        if (!StringUtils.hasText(form.getStatus())) {
            form.setStatus("ENABLED");
        }
        if (form.getId() == null) {
            coldStorageMapper.insert(form);
            operationLogService.log("冷库管理", "新增", "冷库：" + form.getName());
        } else {
            coldStorageMapper.updateById(form);
            operationLogService.log("冷库管理", "编辑", "冷库ID：" + form.getId());
        }
    }

    public void delete(Long id) {
        requireAdmin();
        Long roomCnt = coldRoomMapper.selectCount(new LambdaQueryWrapper<ColdRoom>().eq(ColdRoom::getStorageId, id));
        if (roomCnt != null && roomCnt > 0) {
            throw new BusinessException("请先删除下属冷藏间");
        }
        coldStorageMapper.deleteById(id);
        operationLogService.log("冷库管理", "删除", "冷库ID：" + id);
    }

    private void fillCount(ColdStorage s) {
        Long roomCnt = coldRoomMapper.selectCount(new LambdaQueryWrapper<ColdRoom>().eq(ColdRoom::getStorageId, s.getId()));
        s.setRoomCount(roomCnt == null ? 0L : roomCnt);
        List<ColdRoom> rooms = coldRoomMapper.selectList(new LambdaQueryWrapper<ColdRoom>().eq(ColdRoom::getStorageId, s.getId()));
        if (rooms.isEmpty()) {
            s.setDeviceCount(0L);
            return;
        }
        List<Long> roomIds = rooms.stream().map(ColdRoom::getId).collect(java.util.stream.Collectors.toList());
        Long deviceCnt = deviceMapper.selectCount(new LambdaQueryWrapper<Device>().in(Device::getRoomId, roomIds));
        s.setDeviceCount(deviceCnt == null ? 0L : deviceCnt);
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("无权限");
        }
    }
}
