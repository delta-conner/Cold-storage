package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.*;
import com.coldstorage.oms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

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
    private SparePartMapper sparePartMapper;
    @Autowired
    private MaintainTaskMapper maintainTaskMapper;

    public List<Map<String, Object>> maintainReminders(Integer daysAhead) {
        requireStaff();
        int ahead = daysAhead == null ? 30 : daysAhead;
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .isNotNull(Device::getInstallDate)
                .isNotNull(Device::getMaintainCycleDays));
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Device d : devices) {
            cal.setTime(d.getInstallDate());
            // next maintain = install + n*cycle until >= today
            int cycle = d.getMaintainCycleDays() == null ? 90 : d.getMaintainCycleDays();
            if (cycle <= 0) {
                continue;
            }
            Date next = d.getInstallDate();
            while (next.before(now)) {
                cal.setTime(next);
                cal.add(Calendar.DAY_OF_MONTH, cycle);
                next = cal.getTime();
            }
            long diff = (next.getTime() - now.getTime()) / (24L * 3600 * 1000);
            if (diff <= ahead) {
                Map<String, Object> row = new HashMap<>();
                row.put("deviceId", d.getId());
                row.put("deviceNo", d.getDeviceNo());
                row.put("deviceName", d.getDeviceName());
                row.put("deviceType", d.getDeviceType());
                ColdRoom room = roomMap.get(d.getRoomId());
                row.put("roomName", room == null ? null : room.getName());
                row.put("installDate", sdf.format(d.getInstallDate()));
                row.put("maintainCycleDays", cycle);
                row.put("nextMaintainDate", sdf.format(next));
                row.put("daysLeft", diff);
                row.put("urgent", diff <= 7);
                list.add(row);
            }
        }
        list.sort(Comparator.comparingLong(m -> ((Number) m.get("daysLeft")).longValue()));
        return list;
    }

    public Map<String, Object> adminStats(String month) {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可查看全局统计");
        }
        // month format yyyy-MM, default current
        if (month == null || month.trim().isEmpty()) {
            month = new SimpleDateFormat("yyyy-MM").format(new Date());
        }
        Map<String, Object> result = new HashMap<>();
        List<WorkOrder> allOrders = workOrderMapper.selectList(null);
        long total = allOrders.size();
        long done = allOrders.stream().filter(o -> isFinishedStatus(o.getStatus())).count();
        result.put("orderTotal", total);
        result.put("orderDone", done);
        result.put("orderDoneRate", total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        result.put("month", month);

        // monthly done counts for last 6 months
        List<String> months = new ArrayList<>();
        List<Long> monthlyDone = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat mf = new SimpleDateFormat("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, -i);
            String m = mf.format(cal.getTime());
            months.add(m);
            final String mm = m;
            long c = allOrders.stream()
                    .filter(o -> isFinishedStatus(o.getStatus()) && o.getFinishTime() != null
                            && mf.format(o.getFinishTime()).equals(mm))
                    .count();
            monthlyDone.add(c);
        }
        // also highlight selected month if not in last 6
        result.put("monthLabels", months);
        result.put("monthDoneCounts", monthlyDone);

        List<FaultRecord> faults = faultRecordMapper.selectList(null);
        Map<String, Long> typeCount = faults.stream()
                .collect(Collectors.groupingBy(f -> f.getFaultType() == null ? "其他" : f.getFaultType(), Collectors.counting()));
        List<Map<String, Object>> pie = new ArrayList<>();
        for (Map.Entry<String, Long> e : typeCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            pie.add(item);
        }
        result.put("faultTypePie", pie);

        Map<Long, Long> deviceFaultCount = faults.stream()
                .collect(Collectors.groupingBy(FaultRecord::getDeviceId, Collectors.counting()));
        Map<Long, Device> deviceMap = deviceMapper.selectList(null).stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
        List<Map<String, Object>> rank = deviceFaultCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    Device d = deviceMap.get(e.getKey());
                    item.put("deviceName", d == null ? ("设备" + e.getKey()) : d.getDeviceName());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("deviceFaultRank", rank);

        // selected month done
        final String sel = month;
        long selDone = allOrders.stream()
                .filter(o -> isFinishedStatus(o.getStatus()) && o.getFinishTime() != null
                        && mf.format(o.getFinishTime()).equals(sel))
                .count();
        result.put("selectedMonthDone", selDone);

        // ---- 全系统总结分析补充 ----
        result.put("scope", "SYSTEM_ALL");
        result.put("scopeLabel", "全系统总结分析（跨全部冷库）");

        long pending = allOrders.stream().filter(o -> "PENDING".equals(o.getStatus()) || "ASSIGNED".equals(o.getStatus())).count();
        long processing = allOrders.stream().filter(o -> "PROCESSING".equals(o.getStatus()) || "ACCEPTING".equals(o.getStatus())).count();
        long incomplete = total - done;
        result.put("orderPending", pending);
        result.put("orderProcessing", processing);
        result.put("orderIncomplete", incomplete);

        List<Long> monthlyCreated = new ArrayList<>();
        for (String m : months) {
            final String mm = m;
            long c = allOrders.stream()
                    .filter(o -> o.getCreateTime() != null && mf.format(o.getCreateTime()).equals(mm))
                    .count();
            monthlyCreated.add(c);
        }
        result.put("monthCreatedCounts", monthlyCreated);
        result.put("storageCompare", buildStorageCompare(allOrders, faults));

        // 全局设备健康
        List<Device> allDevices = deviceMapper.selectList(null);
        Map<String, Long> globalDeviceStatus = new LinkedHashMap<>();
        globalDeviceStatus.put("NORMAL", 0L);
        globalDeviceStatus.put("FAULT", 0L);
        globalDeviceStatus.put("REPAIRING", 0L);
        globalDeviceStatus.put("ACCEPTING", 0L);
        globalDeviceStatus.put("SCRAPPED", 0L);
        for (Device d : allDevices) {
            String st = d.getStatus() == null ? "NORMAL" : d.getStatus();
            globalDeviceStatus.put(st, globalDeviceStatus.getOrDefault(st, 0L) + 1);
        }
        result.put("globalDeviceStatus", globalDeviceStatus);
        result.put("deviceTotal", allDevices.size());
        result.put("faultTotal", faults.size());

        // 冷藏间故障排名
        Map<Long, ColdRoom> roomMapAll = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        Map<Long, Long> roomFault = new HashMap<>();
        for (FaultRecord f : faults) {
            Device d = deviceMap.get(f.getDeviceId());
            if (d == null) continue;
            roomFault.put(d.getRoomId(), roomFault.getOrDefault(d.getRoomId(), 0L) + 1);
        }
        List<Map<String, Object>> roomRank = roomFault.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    ColdRoom r = roomMapAll.get(e.getKey());
                    item.put("roomName", r == null ? ("冷藏间" + e.getKey()) : r.getName());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("roomFaultRank", roomRank);

        List<Map<String, Object>> maintain = maintainReminders(30);
        long maintainUrgent = maintain.stream().filter(m -> Boolean.TRUE.equals(m.get("urgent"))).count();
        result.put("maintainNearCount", maintain.size());
        result.put("maintainUrgentCount", maintainUrgent);

        // ---- ⑧ 响应/维修时长、库存、维保任务 ----
        fillDurationMetrics(result, allOrders);
        fillInventoryMetrics(result);
        fillMaintainTaskMetrics(result);

        return result;
    }

    private void fillDurationMetrics(Map<String, Object> result, List<WorkOrder> allOrders) {
        double sumResp = 0;
        int respCnt = 0;
        double sumRepair = 0;
        int repairCnt = 0;
        for (WorkOrder o : allOrders) {
            if (o.getCreateTime() != null && o.getAssignTime() != null) {
                sumResp += (o.getAssignTime().getTime() - o.getCreateTime().getTime()) / 3600000.0;
                respCnt++;
            }
            Date start = o.getAssignTime() != null ? o.getAssignTime() : o.getCreateTime();
            if (start != null && o.getFinishTime() != null && isFinishedStatus(o.getStatus())) {
                sumRepair += (o.getFinishTime().getTime() - start.getTime()) / 3600000.0;
                repairCnt++;
            }
        }
        result.put("avgResponseHours", respCnt == 0 ? 0 : Math.round(sumResp / respCnt * 10) / 10.0);
        result.put("avgRepairHours", repairCnt == 0 ? 0 : Math.round(sumRepair / repairCnt * 10) / 10.0);
        result.put("responseSampleCount", respCnt);
        result.put("repairSampleCount", repairCnt);
    }

    private void fillInventoryMetrics(Map<String, Object> result) {
        List<SparePart> parts = sparePartMapper.selectList(null);
        long low = parts.stream().filter(p -> {
            int stock = p.getStockQty() == null ? 0 : p.getStockQty();
            int safety = p.getSafetyStock() == null ? 0 : p.getSafetyStock();
            return stock < safety;
        }).count();
        result.put("sparePartTotal", parts.size());
        result.put("sparePartLowCount", low);
        result.put("sparePartOkCount", parts.size() - low);

        Map<String, Long> typeStock = new LinkedHashMap<>();
        for (SparePart p : parts) {
            String t = p.getPartType() == null || p.getPartType().isEmpty() ? "未分类" : p.getPartType();
            long qty = p.getStockQty() == null ? 0 : p.getStockQty();
            typeStock.put(t, typeStock.getOrDefault(t, 0L) + qty);
        }
        List<Map<String, Object>> stockPie = new ArrayList<>();
        for (Map.Entry<String, Long> e : typeStock.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            stockPie.add(item);
        }
        result.put("stockTypePie", stockPie);

        List<Map<String, Object>> lowList = parts.stream()
                .filter(p -> {
                    int stock = p.getStockQty() == null ? 0 : p.getStockQty();
                    int safety = p.getSafetyStock() == null ? 0 : p.getSafetyStock();
                    return stock < safety;
                })
                .sorted(Comparator.comparingInt(p -> p.getStockQty() == null ? 0 : p.getStockQty()))
                .limit(8)
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("partName", p.getPartName());
                    item.put("stockQty", p.getStockQty() == null ? 0 : p.getStockQty());
                    item.put("safetyStock", p.getSafetyStock() == null ? 0 : p.getSafetyStock());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("lowStockRank", lowList);
    }

    private void fillMaintainTaskMetrics(Map<String, Object> result) {
        List<MaintainTask> tasks = maintainTaskMapper.selectList(null);
        long pending = tasks.stream().filter(t -> !"DONE".equals(t.getStatus())).count();
        long done = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        long abnormal = tasks.stream().filter(t -> t.getHasAbnormal() != null && t.getHasAbnormal() == 1).count();
        result.put("maintainTaskTotal", tasks.size());
        result.put("maintainTaskPending", pending);
        result.put("maintainTaskDone", done);
        result.put("maintainTaskAbnormal", abnormal);
        List<Map<String, Object>> pie = new ArrayList<>();
        Map<String, Object> a = new HashMap<>();
        a.put("name", "待执行");
        a.put("value", pending);
        pie.add(a);
        Map<String, Object> b = new HashMap<>();
        b.put("name", "已完成");
        b.put("value", done);
        pie.add(b);
        result.put("maintainTaskPie", pie);
    }

    private List<Map<String, Object>> buildStorageCompare(List<WorkOrder> allOrders, List<FaultRecord> faults) {
        List<ColdStorage> storages = coldStorageMapper.selectList(null);
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        Map<Long, Device> deviceMap = deviceMapper.selectList(null).stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ColdStorage s : storages) {
            List<Long> roomIds = roomMap.values().stream()
                    .filter(r -> s.getId().equals(r.getStorageId()))
                    .map(ColdRoom::getId).collect(Collectors.toList());
            long deviceCnt = deviceMap.values().stream().filter(d -> roomIds.contains(d.getRoomId())).count();
            Set<Long> deviceIds = deviceMap.values().stream()
                    .filter(d -> roomIds.contains(d.getRoomId()))
                    .map(Device::getId).collect(Collectors.toSet());
            long openOrders = allOrders.stream()
                    .filter(o -> deviceIds.contains(o.getDeviceId()))
                    .filter(o -> !"DONE".equals(o.getStatus()) && !"EVALUATED".equals(o.getStatus())
                            && !"ARCHIVED".equals(o.getStatus()) && !"CLOSED".equals(o.getStatus()))
                    .count();
            long faultCnt = faults.stream().filter(f -> deviceIds.contains(f.getDeviceId())).count();
            Map<String, Object> row = new HashMap<>();
            row.put("storageName", s.getName());
            row.put("deviceCount", deviceCnt);
            row.put("openOrderCount", openOrders);
            row.put("faultCount", faultCnt);
            list.add(row);
        }
        return list;
    }

    public Map<String, Object> opsDashboard() {
        if (!"OPS".equals(UserContext.getRole()) && !"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("无权限");
        }
        Long uid = UserContext.getUserId();
        List<WorkOrder> mine = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAssigneeId, uid));
        long pending = mine.stream().filter(o -> "ASSIGNED".equals(o.getStatus()) || "PROCESSING".equals(o.getStatus())).count();
        String thisMonth = new SimpleDateFormat("yyyy-MM").format(new Date());
        SimpleDateFormat mf = new SimpleDateFormat("yyyy-MM");
        long monthDone = mine.stream()
                .filter(o -> isFinishedStatus(o.getStatus()) && o.getFinishTime() != null
                        && mf.format(o.getFinishTime()).equals(thisMonth))
                .count();
        Map<String, Object> map = new HashMap<>();
        map.put("processingCount", pending);
        map.put("monthDoneCount", monthDone);
        map.put("maintainList", maintainReminders(30));
        return map;
    }

    private boolean isFinishedStatus(String status) {
        return "DONE".equals(status) || "EVALUATED".equals(status) || "ARCHIVED".equals(status);
    }

    private void requireStaff() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限");
        }
    }
}
