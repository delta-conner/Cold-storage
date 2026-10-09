package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.*;
import com.coldstorage.oms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaintainTaskService {

    @Autowired
    private MaintainTaskMapper taskMapper;
    @Autowired
    private MaintainTaskItemMapper taskItemMapper;
    @Autowired
    private MaintainPlanMapper planMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private MessageService messageService;

    public Page<MaintainTask> page(long page, long size, String status, Long planId, String displayFilter) {
        requireStaff();
        LambdaQueryWrapper<MaintainTask> qw = new LambdaQueryWrapper<>();
        if (planId != null) {
            qw.eq(MaintainTask::getPlanId, planId);
        }
        if ("DONE".equals(status)) {
            qw.eq(MaintainTask::getStatus, "DONE");
        } else if ("PENDING".equals(status)) {
            qw.ne(MaintainTask::getStatus, "DONE");
        }
        // 运维默认看全部任务（可处理指派给自己的）；管理员全量
        qw.orderByAsc(MaintainTask::getDueDate).orderByDesc(MaintainTask::getId);
        Page<MaintainTask> result = taskMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        if (StringUtils.hasText(displayFilter)) {
            List<MaintainTask> filtered = result.getRecords().stream()
                    .filter(t -> displayFilter.equals(t.getDisplayStatus()))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }
        return result;
    }

    public MaintainTask detail(Long id) {
        requireStaff();
        MaintainTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("维保任务不存在");
        }
        fillExtra(Collections.singletonList(task));
        List<MaintainTaskItem> items = taskItemMapper.selectList(new LambdaQueryWrapper<MaintainTaskItem>()
                .eq(MaintainTaskItem::getTaskId, id)
                .orderByAsc(MaintainTaskItem::getSortNo));
        task.setItems(items);
        return task;
    }

    public void assign(Long id, Long assigneeId) {
        requireAdmin();
        MaintainTask task = requireTask(id);
        if ("DONE".equals(task.getStatus())) {
            throw new BusinessException("已完成任务不能指派");
        }
        SysUser ops = userMapper.selectById(assigneeId);
        if (ops == null || !"OPS".equals(ops.getRole())) {
            throw new BusinessException("请选择运维人员");
        }
        task.setAssigneeId(assigneeId);
        taskMapper.updateById(task);
        messageService.sendToUser(ops.getId(), "维保任务指派",
                "您有新的维保任务 " + task.getTaskNo() + "，请按时完成",
                "MAINTAIN_ASSIGN", "MAINTAIN_TASK", task.getId());
        operationLogService.log("维保任务", "指派", task.getTaskNo());
    }

    @Transactional
    public void complete(Long id, MaintainTask form) {
        MaintainTask task = requireTask(id);
        if ("DONE".equals(task.getStatus())) {
            throw new BusinessException("任务已完成");
        }
        String role = UserContext.getRole();
        if ("OPS".equals(role) && task.getAssigneeId() != null
                && !UserContext.getUserId().equals(task.getAssigneeId())) {
            throw new BusinessException("只能完成指派给自己的维保任务");
        }
        if (!"OPS".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException("无权限");
        }

        List<MaintainTaskItem> items = form.getItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException("请填写检查项目结果");
        }
        boolean abnormal = false;
        for (MaintainTaskItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            MaintainTaskItem db = taskItemMapper.selectById(item.getId());
            if (db == null || !Objects.equals(db.getTaskId(), id)) {
                continue;
            }
            if (!StringUtils.hasText(item.getResult())) {
                throw new BusinessException("请完成所有检查项结果：" + db.getItemName());
            }
            db.setResult(item.getResult());
            db.setRemark(item.getRemark());
            taskItemMapper.updateById(db);
            if ("ABNORMAL".equals(item.getResult())) {
                abnormal = true;
            }
        }

        task.setResultSummary(form.getResultSummary());
        task.setMeasures(form.getMeasures());
        task.setPartsUsed(form.getPartsUsed());
        task.setHasAbnormal(abnormal || (form.getHasAbnormal() != null && form.getHasAbnormal() == 1) ? 1 : 0);
        task.setStatus("DONE");
        task.setFinishTime(new Date());
        task.setHandlerId(UserContext.getUserId());
        if (task.getAssigneeId() == null) {
            task.setAssigneeId(UserContext.getUserId());
        }
        taskMapper.updateById(task);
        operationLogService.log("维保任务", "完成", task.getTaskNo());
    }

    public Map<String, Object> summary() {
        requireStaff();
        List<MaintainTask> all = taskMapper.selectList(null);
        fillExtra(all);
        long total = all.size();
        long done = all.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        long overdue = all.stream().filter(t -> "已逾期".equals(t.getDisplayStatus())).count();
        long near = all.stream().filter(t -> "即将到期".equals(t.getDisplayStatus())).count();
        long pending = all.stream().filter(t -> !"DONE".equals(t.getStatus())).count();
        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("done", done);
        map.put("pending", pending);
        map.put("overdue", overdue);
        map.put("nearDue", near);
        map.put("doneRate", total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        return map;
    }

    private MaintainTask requireTask(Long id) {
        MaintainTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("维保任务不存在");
        }
        return task;
    }

    private void fillExtra(List<MaintainTask> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, MaintainPlan> planMap = planMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaintainPlan::getId, p -> p, (a, b) -> a));
        Map<Long, Device> deviceMap = deviceMapper.selectList(null).stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
        Date today = truncate(new Date());
        for (MaintainTask t : list) {
            MaintainPlan plan = planMap.get(t.getPlanId());
            if (plan != null) {
                t.setPlanTitle(plan.getTitle());
            }
            Device d = deviceMap.get(t.getDeviceId());
            if (d != null) {
                t.setDeviceName(d.getDeviceName());
                t.setDeviceNo(d.getDeviceNo());
                t.setDeviceType(d.getDeviceType());
                ColdRoom room = roomMap.get(d.getRoomId());
                if (room != null) {
                    t.setRoomName(room.getName());
                }
            }
            t.setAssigneeName(userMap.get(t.getAssigneeId()));
            t.setHandlerName(userMap.get(t.getHandlerId()));
            applyDisplay(t, today);
        }
    }

    private void applyDisplay(MaintainTask t, Date today) {
        if ("DONE".equals(t.getStatus())) {
            t.setDisplayStatus("已完成");
            t.setDaysLeft(0L);
            return;
        }
        if (t.getDueDate() == null) {
            t.setDisplayStatus("待执行");
            return;
        }
        long days = (t.getDueDate().getTime() - today.getTime()) / (24L * 3600 * 1000);
        t.setDaysLeft(days);
        if (days < 0) {
            t.setDisplayStatus("已逾期");
        } else if (days <= 7) {
            t.setDisplayStatus("即将到期");
        } else {
            t.setDisplayStatus("待执行");
        }
    }

    private Date truncate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private void requireStaff() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限");
        }
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可指派");
        }
    }
}
