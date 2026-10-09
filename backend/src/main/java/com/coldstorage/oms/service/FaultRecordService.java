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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FaultRecordService {

    @Autowired
    private FaultRecordMapper faultRecordMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private OperationLogService operationLogService;

    public List<String> faultTypes() {
        return Arrays.asList(
                "高压异常", "低压异常", "油压异常", "油温异常", "电机过载",
                "温度传感器故障", "压力传感器故障", "融霜异常", "排水异常",
                "风机故障", "电热异常", "通讯异常", "控制器故障", "其他"
        );
    }

    public List<String> faultLevels() {
        return Arrays.asList("一般", "严重", "紧急");
    }

    public Page<FaultRecord> page(long page, long size, Long deviceId, String faultType,
                                  String faultLevel, String startTime, String endTime) {
        requireStaff();
        LambdaQueryWrapper<FaultRecord> qw = new LambdaQueryWrapper<>();
        if (deviceId != null) {
            qw.eq(FaultRecord::getDeviceId, deviceId);
        }
        if (StringUtils.hasText(faultType)) {
            qw.eq(FaultRecord::getFaultType, faultType);
        }
        if (StringUtils.hasText(faultLevel)) {
            qw.eq(FaultRecord::getFaultLevel, faultLevel);
        }
        if (StringUtils.hasText(startTime)) {
            qw.ge(FaultRecord::getHandleTime, startTime);
        }
        if (StringUtils.hasText(endTime)) {
            qw.le(FaultRecord::getHandleTime, endTime);
        }
        qw.orderByDesc(FaultRecord::getId);
        Page<FaultRecord> result = faultRecordMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    public FaultRecord detail(Long id) {
        requireStaff();
        FaultRecord record = faultRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("故障记录不存在");
        }
        fillExtra(Collections.singletonList(record));
        return record;
    }

    public void save(FaultRecord form) {
        requireStaff();
        if (form.getDeviceId() == null || !StringUtils.hasText(form.getFaultType())) {
            throw new BusinessException("设备和故障类型不能为空");
        }
        if (deviceMapper.selectById(form.getDeviceId()) == null) {
            throw new BusinessException("设备不存在");
        }
        if (form.getOrderId() != null && workOrderMapper.selectById(form.getOrderId()) == null) {
            throw new BusinessException("关联工单不存在");
        }
        if (!StringUtils.hasText(form.getFaultLevel())) {
            form.setFaultLevel("一般");
        }
        if (!StringUtils.hasText(form.getFaultPhenomenon()) && StringUtils.hasText(form.getFaultDesc())) {
            form.setFaultPhenomenon(form.getFaultDesc());
        }
        if (form.getId() == null) {
            form.setRecordNo(genNo());
            if (form.getHandlerId() == null) {
                form.setHandlerId(UserContext.getUserId());
            }
            if (form.getHandleTime() == null) {
                form.setHandleTime(new Date());
            }
            faultRecordMapper.insert(form);
            operationLogService.log("故障记录", "新增", form.getRecordNo());
        } else {
            if (!"ADMIN".equals(UserContext.getRole())) {
                FaultRecord db = faultRecordMapper.selectById(form.getId());
                if (db == null) {
                    throw new BusinessException("故障记录不存在");
                }
                if (!UserContext.getUserId().equals(db.getHandlerId())) {
                    throw new BusinessException("只能编辑自己处理的故障记录");
                }
            }
            faultRecordMapper.updateById(form);
            operationLogService.log("故障记录", "编辑", form.getRecordNo());
        }
    }

    public void delete(Long id) {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可删除故障记录");
        }
        FaultRecord db = faultRecordMapper.selectById(id);
        faultRecordMapper.deleteById(id);
        if (db != null) {
            operationLogService.log("故障记录", "删除", db.getRecordNo());
        }
    }

    public void createFromWorkOrder(WorkOrder order, String faultType, String solution, Integer durationMinutes) {
        Long exists = faultRecordMapper.selectCount(new LambdaQueryWrapper<FaultRecord>()
                .eq(FaultRecord::getOrderId, order.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        FaultRecord record = new FaultRecord();
        record.setRecordNo(genNo());
        record.setDeviceId(order.getDeviceId());
        record.setOrderId(order.getId());
        record.setFaultType(StringUtils.hasText(faultType) ? faultType : "其他");
        record.setFaultLevel("一般");
        record.setFaultDesc(order.getFaultDesc());
        record.setFaultPhenomenon(order.getFaultDesc());
        record.setFaultReason(order.getFaultReason());
        record.setSolution(StringUtils.hasText(solution) ? solution
                : (StringUtils.hasText(order.getSolution()) ? order.getSolution() : order.getProcessRecord()));
        record.setDurationMinutes(durationMinutes != null ? durationMinutes : order.getDurationMinutes());
        record.setFaultImages(order.getRepairImages());
        record.setStartTime(order.getAssignTime());
        record.setEndTime(order.getFinishTime() != null ? order.getFinishTime() : new Date());
        record.setHandlerId(order.getAssigneeId() != null ? order.getAssigneeId() : UserContext.getUserId());
        record.setHandleTime(new Date());
        faultRecordMapper.insert(record);

        Device device = deviceMapper.selectById(order.getDeviceId());
        if (device != null) {
            int cnt = device.getFaultCount() == null ? 0 : device.getFaultCount();
            device.setFaultCount(cnt + 1);
            deviceMapper.updateById(device);
        }
    }

    private String genNo() {
        return "FR" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%02d", (int) (Math.random() * 100));
    }

    private void fillExtra(List<FaultRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, Device> deviceMap = deviceMapper.selectList(null).stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
        Map<Long, String> orderMap = workOrderMapper.selectList(null).stream()
                .collect(Collectors.toMap(WorkOrder::getId, WorkOrder::getOrderNo, (a, b) -> a));
        for (FaultRecord r : list) {
            Device d = deviceMap.get(r.getDeviceId());
            if (d != null) {
                r.setDeviceName(d.getDeviceName());
                r.setDeviceNo(d.getDeviceNo());
                r.setDeviceType(d.getDeviceType());
                ColdRoom room = roomMap.get(d.getRoomId());
                if (room != null) {
                    r.setRoomName(room.getName());
                }
            }
            r.setHandlerName(userMap.get(r.getHandlerId()));
            r.setOrderNo(orderMap.get(r.getOrderId()));
        }
    }

    private void requireStaff() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限访问故障记录");
        }
    }
}
