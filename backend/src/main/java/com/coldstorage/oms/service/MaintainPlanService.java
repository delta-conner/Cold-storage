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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaintainPlanService {

    @Autowired
    private MaintainPlanMapper planMapper;
    @Autowired
    private MaintainPlanItemMapper planItemMapper;
    @Autowired
    private MaintainTaskMapper taskMapper;
    @Autowired
    private MaintainTaskItemMapper taskItemMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private OperationLogService operationLogService;

    public List<String> defaultItems(String deviceType) {
        if (deviceType != null && (deviceType.contains("蒸发器") || deviceType.contains("风机"))) {
            return Arrays.asList("风机运转", "结霜情况", "排水通畅", "排水电热", "盘管清洁");
        }
        if (deviceType != null && deviceType.contains("传感器")) {
            return Arrays.asList("探头外观", "接线检查", "读数对比", "报警测试");
        }
        // 主机类默认
        return Arrays.asList("油位检查", "油温检查", "高低压读数", "运行电流", "运行声音");
    }

    public Page<MaintainPlan> page(long page, long size, String status, String cycleType) {
        requireStaff();
        LambdaQueryWrapper<MaintainPlan> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq(MaintainPlan::getStatus, status);
        }
        if (StringUtils.hasText(cycleType)) {
            qw.eq(MaintainPlan::getCycleType, cycleType);
        }
        qw.orderByDesc(MaintainPlan::getId);
        Page<MaintainPlan> result = planMapper.selectPage(new Page<>(page, size), qw);
        fillPlanExtra(result.getRecords());
        return result;
    }

    public MaintainPlan detail(Long id) {
        requireStaff();
        MaintainPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("维保计划不存在");
        }
        fillPlanExtra(Collections.singletonList(plan));
        List<MaintainPlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<MaintainPlanItem>()
                .eq(MaintainPlanItem::getPlanId, id)
                .orderByAsc(MaintainPlanItem::getSortNo));
        plan.setItems(items.stream().map(MaintainPlanItem::getItemName).collect(Collectors.toList()));
        return plan;
    }

    @Transactional
    public void save(MaintainPlan form) {
        requireAdmin();
        if (!StringUtils.hasText(form.getTitle()) || !StringUtils.hasText(form.getCycleType())
                || form.getDueDate() == null) {
            throw new BusinessException("标题、周期类型、到期日不能为空");
        }
        if (!StringUtils.hasText(form.getStatus())) {
            form.setStatus("ACTIVE");
        }
        List<String> items = form.getItems();
        if (items == null || items.isEmpty()) {
            items = defaultItems(form.getDeviceType());
        }
        items = items.stream().filter(StringUtils::hasText).map(String::trim).distinct().collect(Collectors.toList());
        if (items.isEmpty()) {
            throw new BusinessException("请至少配置一项检查项目");
        }

        if (form.getId() == null) {
            form.setPlanNo(genPlanNo());
            form.setCreatorId(UserContext.getUserId());
            planMapper.insert(form);
            savePlanItems(form.getId(), items);
            operationLogService.log("维保计划", "新增", form.getPlanNo());
        } else {
            MaintainPlan db = planMapper.selectById(form.getId());
            if (db == null) {
                throw new BusinessException("维保计划不存在");
            }
            planMapper.updateById(form);
            planItemMapper.delete(new LambdaQueryWrapper<MaintainPlanItem>().eq(MaintainPlanItem::getPlanId, form.getId()));
            savePlanItems(form.getId(), items);
            operationLogService.log("维保计划", "编辑", form.getPlanNo());
        }
    }

    @Transactional
    public int generateTasks(Long planId) {
        requireAdmin();
        MaintainPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("维保计划不存在");
        }
        if ("CLOSED".equals(plan.getStatus())) {
            throw new BusinessException("已关闭计划不能生成任务");
        }
        List<MaintainPlanItem> planItems = planItemMapper.selectList(new LambdaQueryWrapper<MaintainPlanItem>()
                .eq(MaintainPlanItem::getPlanId, planId)
                .orderByAsc(MaintainPlanItem::getSortNo));
        if (planItems.isEmpty()) {
            throw new BusinessException("计划未配置检查项目");
        }

        LambdaQueryWrapper<Device> dq = new LambdaQueryWrapper<Device>()
                .ne(Device::getStatus, "SCRAPPED");
        if (StringUtils.hasText(plan.getDeviceType())) {
            dq.eq(Device::getDeviceType, plan.getDeviceType());
        }
        List<Device> devices = deviceMapper.selectList(dq);
        if (devices.isEmpty()) {
            throw new BusinessException("没有匹配的设备可生成任务");
        }

        int created = 0;
        for (Device d : devices) {
            Long exists = taskMapper.selectCount(new LambdaQueryWrapper<MaintainTask>()
                    .eq(MaintainTask::getPlanId, planId)
                    .eq(MaintainTask::getDeviceId, d.getId())
                    .ne(MaintainTask::getStatus, "DONE"));
            if (exists != null && exists > 0) {
                continue;
            }
            MaintainTask task = new MaintainTask();
            task.setTaskNo(genTaskNo());
            task.setPlanId(planId);
            task.setDeviceId(d.getId());
            task.setDueDate(plan.getDueDate());
            task.setStatus("PENDING");
            if (d.getOwnerOpsId() != null) {
                task.setAssigneeId(d.getOwnerOpsId());
            }
            taskMapper.insert(task);
            int sort = 1;
            for (MaintainPlanItem pi : planItems) {
                MaintainTaskItem ti = new MaintainTaskItem();
                ti.setTaskId(task.getId());
                ti.setItemName(pi.getItemName());
                ti.setSortNo(sort++);
                taskItemMapper.insert(ti);
            }
            created++;
        }
        if (created == 0) {
            throw new BusinessException("匹配设备均已有未完成任务，未新建");
        }
        operationLogService.log("维保计划", "生成任务", plan.getPlanNo() + " ×" + created);
        return created;
    }

    public void close(Long id) {
        requireAdmin();
        MaintainPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("维保计划不存在");
        }
        plan.setStatus("CLOSED");
        planMapper.updateById(plan);
        operationLogService.log("维保计划", "关闭", plan.getPlanNo());
    }

    public void delete(Long id) {
        requireAdmin();
        Long taskCnt = taskMapper.selectCount(new LambdaQueryWrapper<MaintainTask>().eq(MaintainTask::getPlanId, id));
        if (taskCnt != null && taskCnt > 0) {
            throw new BusinessException("已有维保任务，不能删除，请先关闭计划");
        }
        planItemMapper.delete(new LambdaQueryWrapper<MaintainPlanItem>().eq(MaintainPlanItem::getPlanId, id));
        planMapper.deleteById(id);
        operationLogService.log("维保计划", "删除", "计划ID：" + id);
    }

    private void savePlanItems(Long planId, List<String> items) {
        int sort = 1;
        for (String name : items) {
            MaintainPlanItem item = new MaintainPlanItem();
            item.setPlanId(planId);
            item.setItemName(name);
            item.setSortNo(sort++);
            planItemMapper.insert(item);
        }
    }

    private void fillPlanExtra(List<MaintainPlan> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
        Date today = truncate(new Date());
        for (MaintainPlan p : list) {
            p.setCreatorName(userMap.get(p.getCreatorId()));
            Long total = taskMapper.selectCount(new LambdaQueryWrapper<MaintainTask>().eq(MaintainTask::getPlanId, p.getId()));
            Long done = taskMapper.selectCount(new LambdaQueryWrapper<MaintainTask>()
                    .eq(MaintainTask::getPlanId, p.getId()).eq(MaintainTask::getStatus, "DONE"));
            p.setTaskTotal(total == null ? 0 : total.intValue());
            p.setTaskDone(done == null ? 0 : done.intValue());
            p.setDisplayStatus(calcPlanDisplay(p, today));
        }
    }

    private String calcPlanDisplay(MaintainPlan p, Date today) {
        if ("CLOSED".equals(p.getStatus())) {
            return "已关闭";
        }
        if (p.getTaskTotal() != null && p.getTaskTotal() > 0
                && Objects.equals(p.getTaskTotal(), p.getTaskDone())) {
            return "已完成";
        }
        if (p.getDueDate() != null && p.getDueDate().before(today)) {
            return "已逾期";
        }
        if (p.getDueDate() != null) {
            long days = (p.getDueDate().getTime() - today.getTime()) / (24L * 3600 * 1000);
            if (days <= 7) {
                return "即将到期";
            }
        }
        return "进行中";
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

    private String genPlanNo() {
        return "MP" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%02d", (int) (Math.random() * 100));
    }

    private String genTaskNo() {
        return "MT" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%02d", (int) (Math.random() * 100));
    }

    private void requireStaff() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限");
        }
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可操作维保计划");
        }
    }
}
