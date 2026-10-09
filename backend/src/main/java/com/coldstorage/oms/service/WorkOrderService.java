package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.entity.ColdStorage;
import com.coldstorage.oms.entity.Device;
import com.coldstorage.oms.entity.SparePart;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.entity.WorkOrder;
import com.coldstorage.oms.entity.WorkOrderPart;
import com.coldstorage.oms.mapper.ColdRoomMapper;
import com.coldstorage.oms.mapper.ColdStorageMapper;
import com.coldstorage.oms.mapper.DeviceMapper;
import com.coldstorage.oms.mapper.SparePartMapper;
import com.coldstorage.oms.mapper.SysUserMapper;
import com.coldstorage.oms.mapper.WorkOrderMapper;
import com.coldstorage.oms.mapper.WorkOrderPartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单七状态：
 * 待受理 PENDING → 已派单 ASSIGNED → 处理中 PROCESSING → 待验收 ACCEPTING
 * → 已完成 DONE → 已评价 EVALUATED → 已归档 ARCHIVED
 */
@Service
public class WorkOrderService {

    private static final Set<String> OPEN_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING", "ASSIGNED", "PROCESSING", "ACCEPTING"
    ));

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private ColdStorageMapper coldStorageMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private FaultRecordService faultRecordService;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private SparePartService sparePartService;
    @Autowired
    private WorkOrderPartMapper workOrderPartMapper;
    @Autowired
    private SparePartMapper sparePartMapper;
    @Autowired
    private MessageService messageService;

    public Page<WorkOrder> page(long page, long size, String status, String orderNo, Long deviceId) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        String role = UserContext.getRole();
        if ("CLIENT".equals(role)) {
            qw.eq(WorkOrder::getSubmitUserId, UserContext.getUserId());
        }
        if (StringUtils.hasText(status)) {
            qw.eq(WorkOrder::getStatus, status);
        }
        if (StringUtils.hasText(orderNo)) {
            qw.like(WorkOrder::getOrderNo, orderNo);
        }
        if (deviceId != null) {
            qw.eq(WorkOrder::getDeviceId, deviceId);
        }
        qw.orderByDesc(WorkOrder::getId);
        Page<WorkOrder> result = workOrderMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    public WorkOrder detail(Long id) {
        WorkOrder order = requireOrder(id);
        if ("CLIENT".equals(UserContext.getRole())
                && !UserContext.getUserId().equals(order.getSubmitUserId())) {
            throw new BusinessException("无权查看该工单");
        }
        fillExtra(java.util.Collections.singletonList(order));
        order.setParts(loadParts(id));
        return order;
    }

    @Transactional
    public void submit(WorkOrder form) {
        if (!"CLIENT".equals(UserContext.getRole()) && !"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅甲方可提交报修");
        }
        if (form.getDeviceId() == null || !StringUtils.hasText(form.getFaultDesc())) {
            throw new BusinessException("设备和故障描述不能为空");
        }
        Device device = deviceMapper.selectById(form.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if ("SCRAPPED".equals(device.getStatus())) {
            throw new BusinessException("报废设备不可报修");
        }
        WorkOrder order = new WorkOrder();
        order.setOrderNo(genOrderNo());
        order.setDeviceId(form.getDeviceId());
        order.setFaultDesc(form.getFaultDesc());
        order.setFaultImages(trimImages(form.getFaultImages()));
        order.setStatus("PENDING");
        order.setSubmitUserId(UserContext.getUserId());
        workOrderMapper.insert(order);
        deviceService.applyStatusFromBiz(device.getId(), "FAULT", "工单报修 " + order.getOrderNo());
        messageService.sendToAdmins("新报修工单", "工单 " + order.getOrderNo() + " 待受理：" + order.getFaultDesc(),
                "ORDER_NEW", "WORK_ORDER", order.getId());
        operationLogService.log("工单管理", "报修", "工单：" + order.getOrderNo());
    }

    /** 派单：待受理 → 已派单 */
    @Transactional
    public void assign(Long id, Long assigneeId) {
        requireAdmin();
        WorkOrder order = requireOrder(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("仅待受理工单可派单");
        }
        SysUser ops = requireOpsUser(assigneeId);
        order.setAssigneeId(ops.getId());
        order.setStatus("ASSIGNED");
        order.setAssignTime(new Date());
        workOrderMapper.updateById(order);
        messageService.sendToUser(ops.getId(), "工单派单提醒",
                "您有新的工单 " + order.getOrderNo() + "，请及时接单处理",
                "ORDER_ASSIGN", "WORK_ORDER", order.getId());
        operationLogService.log("工单管理", "派单", order.getOrderNo() + " → " + ops.getUsername());
    }

    /** 转派：已派单/处理中可换人 */
    @Transactional
    public void transfer(Long id, Long assigneeId) {
        requireAdmin();
        WorkOrder order = requireOrder(id);
        if (!"ASSIGNED".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            throw new BusinessException("仅已派单或处理中的工单可转派");
        }
        SysUser ops = requireOpsUser(assigneeId);
        if (ops.getId().equals(order.getAssigneeId())) {
            throw new BusinessException("转派对象与当前运维相同");
        }
        order.setAssigneeId(ops.getId());
        order.setAssignTime(new Date());
        // 转派后回到已派单，需新运维重新接单
        order.setStatus("ASSIGNED");
        workOrderMapper.updateById(order);
        messageService.sendToUser(ops.getId(), "工单转派提醒",
                "工单 " + order.getOrderNo() + " 已转派给您",
                "ORDER_ASSIGN", "WORK_ORDER", order.getId());
        operationLogService.log("工单管理", "转派", order.getOrderNo() + " → " + ops.getUsername());
    }

    /** 撤回：已派单 → 待受理 */
    @Transactional
    public void withdraw(Long id) {
        requireAdmin();
        WorkOrder order = requireOrder(id);
        if (!"ASSIGNED".equals(order.getStatus())) {
            throw new BusinessException("仅已派单工单可撤回");
        }
        workOrderMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getId, id)
                .set(WorkOrder::getAssigneeId, null)
                .set(WorkOrder::getAssignTime, null)
                .set(WorkOrder::getStatus, "PENDING"));
        operationLogService.log("工单管理", "撤回", order.getOrderNo());
    }

    /** 运维接单：已派单 → 处理中 */
    @Transactional
    public void start(Long id) {
        WorkOrder order = requireOrder(id);
        requireAssigneeOrAdmin(order);
        if (!"ASSIGNED".equals(order.getStatus())) {
            throw new BusinessException("仅已派单工单可接单");
        }
        order.setStatus("PROCESSING");
        workOrderMapper.updateById(order);
        deviceService.applyStatusFromBiz(order.getDeviceId(), "REPAIRING", "工单接单 " + order.getOrderNo());
        operationLogService.log("工单管理", "接单", order.getOrderNo());
    }

    /** 保存处理过程（仍处理中）或提交完工进入待验收 */
    @Transactional
    public void process(Long id, String processRecord, boolean finish,
                        String faultType, String faultReason, String solution,
                        Integer durationMinutes, String repairImages,
                        List<Map<String, Object>> parts) {
        WorkOrder order = requireOrder(id);
        requireAssigneeOrAdmin(order);
        if (!"PROCESSING".equals(order.getStatus())) {
            throw new BusinessException("工单状态不允许处理");
        }
        if (StringUtils.hasText(processRecord)) {
            order.setProcessRecord(processRecord);
        }
        if (StringUtils.hasText(faultType)) {
            order.setFaultType(faultType);
        }
        if (StringUtils.hasText(faultReason)) {
            order.setFaultReason(faultReason);
        }
        if (StringUtils.hasText(solution)) {
            order.setSolution(solution);
        }
        if (durationMinutes != null) {
            order.setDurationMinutes(durationMinutes);
        }
        if (repairImages != null) {
            order.setRepairImages(trimImages(repairImages));
        }
        if (finish) {
            if (!StringUtils.hasText(order.getProcessRecord()) && !StringUtils.hasText(order.getSolution())) {
                throw new BusinessException("提交完工前请填写处理记录或处理方案");
            }
            // 领用备件并扣库存（仅提交完工时）
            applyOrderParts(id, order.getOrderNo(), parts);
            order.setStatus("ACCEPTING");
            order.setFinishTime(new Date());
            workOrderMapper.updateById(order);
            deviceService.applyStatusFromBiz(order.getDeviceId(), "ACCEPTING", "工单提交验收 " + order.getOrderNo());
            faultRecordService.createFromWorkOrder(
                    order,
                    order.getFaultType(),
                    StringUtils.hasText(order.getSolution()) ? order.getSolution() : order.getProcessRecord(),
                    order.getDurationMinutes());
            operationLogService.log("工单管理", "提交验收", order.getOrderNo());
            messageService.sendToUser(order.getSubmitUserId(), "工单待验收",
                    "工单 " + order.getOrderNo() + " 已处理完成，请确认验收",
                    "ORDER_ACCEPTING", "WORK_ORDER", order.getId());
            return;
        }
        workOrderMapper.updateById(order);
        operationLogService.log("工单管理", "保存处理", order.getOrderNo());
    }

    private void applyOrderParts(Long orderId, String orderNo, List<Map<String, Object>> parts) {
        Long exists = workOrderPartMapper.selectCount(new LambdaQueryWrapper<WorkOrderPart>()
                .eq(WorkOrderPart::getOrderId, orderId));
        if (exists != null && exists > 0) {
            return;
        }
        if (parts == null || parts.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : parts) {
            if (row.get("partId") == null || row.get("quantity") == null) {
                continue;
            }
            Long partId = Long.valueOf(String.valueOf(row.get("partId")));
            int qty = Integer.parseInt(String.valueOf(row.get("quantity")));
            if (qty <= 0) {
                continue;
            }
            sparePartService.consumeOut(partId, qty, "WORK_ORDER", orderId, "工单领用 " + orderNo);
            WorkOrderPart wop = new WorkOrderPart();
            wop.setOrderId(orderId);
            wop.setPartId(partId);
            wop.setQuantity(qty);
            workOrderPartMapper.insert(wop);
        }
    }

    private List<WorkOrderPart> loadParts(Long orderId) {
        List<WorkOrderPart> list = workOrderPartMapper.selectList(new LambdaQueryWrapper<WorkOrderPart>()
                .eq(WorkOrderPart::getOrderId, orderId)
                .orderByAsc(WorkOrderPart::getId));
        if (list.isEmpty()) {
            return list;
        }
        Map<Long, SparePart> map = sparePartMapper.selectList(null).stream()
                .collect(Collectors.toMap(SparePart::getId, p -> p, (a, b) -> a));
        for (WorkOrderPart p : list) {
            SparePart sp = map.get(p.getPartId());
            if (sp != null) {
                p.setPartNo(sp.getPartNo());
                p.setPartName(sp.getPartName());
                p.setUnit(sp.getUnit());
            }
        }
        return list;
    }

    /** 甲方确认验收：待验收 → 已完成 */
    @Transactional
    public void accept(Long id, String acceptRemark) {
        WorkOrder order = requireOrder(id);
        if (!"CLIENT".equals(UserContext.getRole()) && !"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅报修方可确认验收");
        }
        if ("CLIENT".equals(UserContext.getRole())
                && !UserContext.getUserId().equals(order.getSubmitUserId())) {
            throw new BusinessException("只能验收自己的工单");
        }
        if (!"ACCEPTING".equals(order.getStatus())) {
            throw new BusinessException("仅待验收工单可确认完工");
        }
        order.setAcceptRemark(acceptRemark);
        order.setAcceptTime(new Date());
        order.setStatus("DONE");
        workOrderMapper.updateById(order);
        deviceService.applyStatusFromBiz(order.getDeviceId(), "NORMAL", "工单验收通过 " + order.getOrderNo());
        operationLogService.log("工单管理", "验收", order.getOrderNo());
        if (order.getAssigneeId() != null) {
            messageService.sendToUser(order.getAssigneeId(), "工单已验收完成",
                    "工单 " + order.getOrderNo() + " 甲方已确认验收",
                    "ORDER_DONE", "WORK_ORDER", order.getId());
        }
    }

    /** 评价：已完成 → 已评价 */
    @Transactional
    public void evaluate(Long id, Integer satisfaction, String content) {
        if (!"CLIENT".equals(UserContext.getRole())) {
            throw new BusinessException("仅甲方可评价");
        }
        WorkOrder order = requireOrder(id);
        if (!UserContext.getUserId().equals(order.getSubmitUserId())) {
            throw new BusinessException("只能评价自己的工单");
        }
        if (!"DONE".equals(order.getStatus())) {
            throw new BusinessException("仅已完成工单可评价");
        }
        if (satisfaction == null || satisfaction < 1 || satisfaction > 5) {
            throw new BusinessException("请选择1-5分满意度");
        }
        order.setSatisfaction(satisfaction);
        order.setEvaluateContent(content);
        order.setEvaluateTime(new Date());
        order.setStatus("EVALUATED");
        workOrderMapper.updateById(order);
        operationLogService.log("工单管理", "评价", order.getOrderNo());
    }

    /** 归档：已完成/已评价 → 已归档 */
    @Transactional
    public void archive(Long id) {
        requireAdmin();
        WorkOrder order = requireOrder(id);
        if (!"DONE".equals(order.getStatus()) && !"EVALUATED".equals(order.getStatus())) {
            throw new BusinessException("仅已完成或已评价工单可归档");
        }
        order.setStatus("ARCHIVED");
        order.setArchiveTime(new Date());
        workOrderMapper.updateById(order);
        operationLogService.log("工单管理", "归档", order.getOrderNo());
    }

    /** 关闭：未走完流程的提前结束 → 已归档 */
    @Transactional
    public void close(Long id, String reason) {
        requireAdmin();
        WorkOrder order = requireOrder(id);
        if (!OPEN_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("当前状态不可关闭");
        }
        order.setCloseReason(StringUtils.hasText(reason) ? reason : "管理员关闭");
        order.setStatus("ARCHIVED");
        order.setArchiveTime(new Date());
        workOrderMapper.updateById(order);
        deviceService.applyStatusFromBiz(order.getDeviceId(), "NORMAL", "工单关闭 " + order.getOrderNo());
        operationLogService.log("工单管理", "关闭", order.getOrderNo());
    }

    /** 重开：已归档 → 待受理 */
    @Transactional
    public void reopen(Long id) {
        requireAdmin();
        WorkOrder order = requireOrder(id);
        if (!"ARCHIVED".equals(order.getStatus())) {
            throw new BusinessException("仅已归档工单可重开");
        }
        workOrderMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getId, id)
                .set(WorkOrder::getAssigneeId, null)
                .set(WorkOrder::getAssignTime, null)
                .set(WorkOrder::getFinishTime, null)
                .set(WorkOrder::getAcceptTime, null)
                .set(WorkOrder::getArchiveTime, null)
                .set(WorkOrder::getCloseReason, null)
                .set(WorkOrder::getStatus, "PENDING"));
        deviceService.applyStatusFromBiz(order.getDeviceId(), "FAULT", "工单重开 " + order.getOrderNo());
        operationLogService.log("工单管理", "重开", order.getOrderNo());
    }

    private String trimImages(String images) {
        if (!StringUtils.hasText(images)) {
            return null;
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .limit(9)
                .collect(Collectors.joining(","));
    }

    private WorkOrder requireOrder(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        return order;
    }

    private SysUser requireOpsUser(Long assigneeId) {
        SysUser ops = userMapper.selectById(assigneeId);
        if (ops == null || !"OPS".equals(ops.getRole())) {
            throw new BusinessException("请选择运维人员");
        }
        if (ops.getStatus() != null && ops.getStatus() == 0) {
            throw new BusinessException("该运维账号已禁用");
        }
        return ops;
    }

    private void requireAssigneeOrAdmin(WorkOrder order) {
        String role = UserContext.getRole();
        if ("ADMIN".equals(role)) {
            return;
        }
        if (!"OPS".equals(role)) {
            throw new BusinessException("无权限处理工单");
        }
        if (!UserContext.getUserId().equals(order.getAssigneeId())) {
            throw new BusinessException("只能处理指派给自己的工单");
        }
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可操作");
        }
    }

    private String genOrderNo() {
        return "WO" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%02d", (int) (Math.random() * 100));
    }

    private void fillExtra(List<WorkOrder> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, Device> deviceMap = deviceMapper.selectList(null).stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        Map<Long, ColdStorage> storageMap = coldStorageMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdStorage::getId, s -> s, (a, b) -> a));
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
        for (WorkOrder o : list) {
            Device d = deviceMap.get(o.getDeviceId());
            if (d != null) {
                o.setDeviceName(d.getDeviceName());
                o.setDeviceNo(d.getDeviceNo());
                ColdRoom room = roomMap.get(d.getRoomId());
                if (room != null) {
                    o.setRoomName(room.getName());
                    ColdStorage st = storageMap.get(room.getStorageId());
                    if (st != null) {
                        o.setStorageName(st.getName());
                    }
                }
            }
            o.setSubmitUserName(userMap.get(o.getSubmitUserId()));
            o.setAssigneeName(userMap.get(o.getAssigneeId()));
        }
    }
}
