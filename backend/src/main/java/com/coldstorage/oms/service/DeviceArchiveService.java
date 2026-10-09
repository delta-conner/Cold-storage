package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.*;
import com.coldstorage.oms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备维修档案汇总 + 规则式健康分析（非 AI）
 */
@Service
public class DeviceArchiveService {

    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private ColdStorageMapper coldStorageMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private FaultRecordMapper faultRecordMapper;
    @Autowired
    private MaintainTaskMapper maintainTaskMapper;
    @Autowired
    private WorkOrderPartMapper workOrderPartMapper;
    @Autowired
    private SparePartMapper sparePartMapper;
    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;

    public Map<String, Object> archive(Long deviceId) {
        requireLogin();
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        fillDeviceRoom(device);
        if ("CLIENT".equals(UserContext.getRole())) {
            device.setRemark(null);
        }

        List<WorkOrder> orders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getDeviceId, deviceId)
                .orderByDesc(WorkOrder::getId)
                .last("LIMIT 50"));
        List<FaultRecord> faults = faultRecordMapper.selectList(new LambdaQueryWrapper<FaultRecord>()
                .eq(FaultRecord::getDeviceId, deviceId)
                .orderByDesc(FaultRecord::getId)
                .last("LIMIT 50"));
        List<MaintainTask> tasks = maintainTaskMapper.selectList(new LambdaQueryWrapper<MaintainTask>()
                .eq(MaintainTask::getDeviceId, deviceId)
                .orderByDesc(MaintainTask::getId)
                .last("LIMIT 50"));

        List<Long> orderIds = orders.stream().map(WorkOrder::getId).collect(Collectors.toList());
        List<WorkOrderPart> parts = orderIds.isEmpty() ? Collections.emptyList()
                : workOrderPartMapper.selectList(new LambdaQueryWrapper<WorkOrderPart>()
                .in(WorkOrderPart::getOrderId, orderIds)
                .orderByDesc(WorkOrderPart::getId));
        Map<Long, SparePart> partMap = sparePartMapper.selectList(null).stream()
                .collect(Collectors.toMap(SparePart::getId, p -> p, (a, b) -> a));
        Map<Long, String> orderNoMap = orders.stream()
                .collect(Collectors.toMap(WorkOrder::getId, WorkOrder::getOrderNo, (a, b) -> a));
        List<Map<String, Object>> partRows = new ArrayList<>();
        int partsQty = 0;
        for (WorkOrderPart wp : parts) {
            SparePart sp = partMap.get(wp.getPartId());
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", wp.getOrderId());
            row.put("orderNo", orderNoMap.get(wp.getOrderId()));
            row.put("partId", wp.getPartId());
            row.put("partNo", sp == null ? null : sp.getPartNo());
            row.put("partName", sp == null ? null : sp.getPartName());
            row.put("quantity", wp.getQuantity());
            row.put("unit", sp == null ? null : sp.getUnit());
            row.put("createTime", wp.getCreateTime());
            partRows.add(row);
            partsQty += wp.getQuantity() == null ? 0 : wp.getQuantity();
        }

        Map<String, Object> health = computeHealth(device, faults, orders, tasks, partsQty);
        List<DeviceStatusLog> statusLogs = deviceStatusLogMapper.selectList(new LambdaQueryWrapper<DeviceStatusLog>()
                .eq(DeviceStatusLog::getDeviceId, deviceId)
                .orderByDesc(DeviceStatusLog::getId)
                .last("LIMIT 20"));

        Map<String, Object> result = new HashMap<>();
        result.put("device", device);
        result.put("summary", health);
        result.put("orders", simplifyOrders(orders));
        result.put("faults", simplifyFaults(faults));
        result.put("maintainTasks", simplifyTasks(tasks));
        result.put("parts", partRows);
        result.put("statusLogs", statusLogs);
        return result;
    }

    public Page<Map<String, Object>> healthPage(long page, long size, String healthLevel,
                                                String deviceName, Long roomId) {
        requireLogin();
        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deviceName)) {
            qw.like(Device::getDeviceName, deviceName);
        }
        if (roomId != null) {
            qw.eq(Device::getRoomId, roomId);
        }
        qw.ne(Device::getStatus, "SCRAPPED");
        qw.orderByDesc(Device::getId);
        // 先拉全量再过滤健康等级（毕设数据量小）
        List<Device> all = deviceMapper.selectList(qw);
        for (Device d : all) {
            fillDeviceRoom(d);
        }

        List<FaultRecord> allFaults = faultRecordMapper.selectList(null);
        List<WorkOrder> allOrders = workOrderMapper.selectList(null);
        List<MaintainTask> allTasks = maintainTaskMapper.selectList(null);
        List<WorkOrderPart> allParts = workOrderPartMapper.selectList(null);

        Map<Long, List<FaultRecord>> faultByDev = allFaults.stream()
                .collect(Collectors.groupingBy(FaultRecord::getDeviceId));
        Map<Long, List<WorkOrder>> orderByDev = allOrders.stream()
                .collect(Collectors.groupingBy(WorkOrder::getDeviceId));
        Map<Long, List<MaintainTask>> taskByDev = allTasks.stream()
                .collect(Collectors.groupingBy(MaintainTask::getDeviceId));
        Map<Long, Integer> partsByOrder = allParts.stream()
                .collect(Collectors.groupingBy(WorkOrderPart::getOrderId,
                        Collectors.summingInt(p -> p.getQuantity() == null ? 0 : p.getQuantity())));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Device d : all) {
            List<FaultRecord> faults = faultByDev.getOrDefault(d.getId(), Collections.emptyList());
            List<WorkOrder> orders = orderByDev.getOrDefault(d.getId(), Collections.emptyList());
            List<MaintainTask> tasks = taskByDev.getOrDefault(d.getId(), Collections.emptyList());
            int partsQty = 0;
            for (WorkOrder o : orders) {
                partsQty += partsByOrder.getOrDefault(o.getId(), 0);
            }
            Map<String, Object> health = computeHealth(d, faults, orders, tasks, partsQty);
            if (StringUtils.hasText(healthLevel) && !healthLevel.equals(health.get("healthLevel"))) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("deviceId", d.getId());
            row.put("deviceNo", d.getDeviceNo());
            row.put("deviceName", d.getDeviceName());
            row.put("deviceType", d.getDeviceType());
            row.put("roomName", d.getRoomName());
            row.put("storageName", d.getStorageName());
            row.put("status", d.getStatus());
            row.putAll(health);
            rows.add(row);
        }

        // 预警优先
        rows.sort((a, b) -> {
            int wa = levelWeight(String.valueOf(a.get("healthLevel")));
            int wb = levelWeight(String.valueOf(b.get("healthLevel")));
            if (wa != wb) {
                return Integer.compare(wb, wa);
            }
            return Long.compare(
                    ((Number) b.getOrDefault("yearFaultCount", 0)).longValue(),
                    ((Number) a.getOrDefault("yearFaultCount", 0)).longValue());
        });

        int from = (int) Math.max(0, (page - 1) * size);
        int to = (int) Math.min(rows.size(), from + size);
        List<Map<String, Object>> pageList = from >= rows.size() ? Collections.emptyList() : rows.subList(from, to);
        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setTotal(rows.size());
        result.setRecords(pageList);

        Map<String, Long> dist = rows.stream()
                .collect(Collectors.groupingBy(r -> String.valueOf(r.get("healthLevel")), Collectors.counting()));
        // 把分布塞进第一页额外字段不方便，用单独 summary 接口更干净；这里附加在空壳不行。
        // 前端可再调 /health/summary
        return result;
    }

    public Map<String, Object> healthSummary() {
        requireLogin();
        Page<Map<String, Object>> page = healthPage(1, 10000, null, null, null);
        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("NORMAL", 0L);
        dist.put("WATCH", 0L);
        dist.put("WARN", 0L);
        for (Map<String, Object> r : page.getRecords()) {
            String lv = String.valueOf(r.get("healthLevel"));
            dist.put(lv, dist.getOrDefault(lv, 0L) + 1);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("total", page.getTotal());
        map.put("normalCount", dist.get("NORMAL"));
        map.put("watchCount", dist.get("WATCH"));
        map.put("warnCount", dist.get("WARN"));
        return map;
    }

    private Map<String, Object> computeHealth(Device device, List<FaultRecord> faults,
                                              List<WorkOrder> orders, List<MaintainTask> tasks,
                                              int partsQty) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, -1);
        Date yearAgo = cal.getTime();

        List<FaultRecord> yearFaults = faults.stream()
                .filter(f -> f.getHandleTime() != null && !f.getHandleTime().before(yearAgo))
                .collect(Collectors.toList());
        int yearFaultCount = yearFaults.size();
        // 维修次数：一年内已完成/已评价/已归档/待验收的维修工单
        long repairCount = orders.stream()
                .filter(o -> o.getFinishTime() != null && !o.getFinishTime().before(yearAgo))
                .filter(o -> Arrays.asList("ACCEPTING", "DONE", "EVALUATED", "ARCHIVED").contains(o.getStatus()))
                .count();
        long maintainCount = tasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getFinishTime() != null && !t.getFinishTime().before(yearAgo))
                .count();

        Date lastFaultTime = faults.stream()
                .map(FaultRecord::getHandleTime)
                .filter(Objects::nonNull)
                .max(Date::compareTo)
                .orElse(null);

        // 平均维修时长（分钟）
        DoubleSummaryStatistics durationStats = faults.stream()
                .filter(f -> f.getDurationMinutes() != null && f.getDurationMinutes() > 0)
                .mapToDouble(FaultRecord::getDurationMinutes)
                .summaryStatistics();
        double avgRepairMinutes = durationStats.getCount() == 0 ? 0
                : Math.round(durationStats.getAverage() * 10) / 10.0;

        // 平均故障间隔（天）：按故障时间排序算间隔均值
        List<Date> faultTimes = yearFaults.stream()
                .map(FaultRecord::getHandleTime)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        double avgFaultIntervalDays = 0;
        if (faultTimes.size() >= 2) {
            long sum = 0;
            for (int i = 1; i < faultTimes.size(); i++) {
                sum += (faultTimes.get(i).getTime() - faultTimes.get(i - 1).getTime()) / (24L * 3600 * 1000);
            }
            avgFaultIntervalDays = Math.round((sum * 10.0) / (faultTimes.size() - 1)) / 10.0;
        }

        String healthLevel;
        String healthLabel;
        if (yearFaultCount <= 2) {
            healthLevel = "NORMAL";
            healthLabel = "正常";
        } else if (yearFaultCount <= 5) {
            healthLevel = "WATCH";
            healthLabel = "关注";
        } else {
            healthLevel = "WARN";
            healthLabel = "预警";
        }

        Map<String, Object> map = new HashMap<>();
        map.put("faultCountTotal", faults.size());
        map.put("yearFaultCount", yearFaultCount);
        map.put("repairCount", repairCount);
        map.put("maintainCount", maintainCount);
        map.put("partsUsedCount", partsQty);
        map.put("lastFaultTime", lastFaultTime);
        map.put("avgRepairMinutes", avgRepairMinutes);
        map.put("avgFaultIntervalDays", avgFaultIntervalDays);
        map.put("healthLevel", healthLevel);
        map.put("healthLabel", healthLabel);
        map.put("healthRule", "近一年故障≤2正常；3～5关注；>5预警");
        map.put("deviceFaultCountField", device.getFaultCount());
        return map;
    }

    private int levelWeight(String level) {
        if ("WARN".equals(level)) return 3;
        if ("WATCH".equals(level)) return 2;
        return 1;
    }

    private List<Map<String, Object>> simplifyOrders(List<WorkOrder> orders) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WorkOrder o : orders) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("faultDesc", o.getFaultDesc());
            m.put("createTime", o.getCreateTime());
            m.put("finishTime", o.getFinishTime());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> simplifyFaults(List<FaultRecord> faults) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FaultRecord f : faults) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("recordNo", f.getRecordNo());
            m.put("faultType", f.getFaultType());
            m.put("faultLevel", f.getFaultLevel());
            m.put("faultPhenomenon", f.getFaultPhenomenon() != null ? f.getFaultPhenomenon() : f.getFaultDesc());
            m.put("solution", f.getSolution());
            m.put("durationMinutes", f.getDurationMinutes());
            m.put("handleTime", f.getHandleTime());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> simplifyTasks(List<MaintainTask> tasks) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MaintainTask t : tasks) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("taskNo", t.getTaskNo());
            m.put("status", t.getStatus());
            m.put("dueDate", t.getDueDate());
            m.put("finishTime", t.getFinishTime());
            m.put("hasAbnormal", t.getHasAbnormal());
            m.put("resultSummary", t.getResultSummary());
            list.add(m);
        }
        return list;
    }

    private void fillDeviceRoom(Device d) {
        if (d.getRoomId() == null) {
            return;
        }
        ColdRoom room = coldRoomMapper.selectById(d.getRoomId());
        if (room != null) {
            d.setRoomName(room.getName());
            d.setStorageId(room.getStorageId());
            ColdStorage st = coldStorageMapper.selectById(room.getStorageId());
            if (st != null) {
                d.setStorageName(st.getName());
            }
        }
    }

    private void requireLogin() {
        if (UserContext.getUserId() == null) {
            throw new BusinessException("未登录");
        }
    }
}
