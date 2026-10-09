package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.Device;
import com.coldstorage.oms.entity.FaultCase;
import com.coldstorage.oms.entity.FaultRecord;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.mapper.DeviceMapper;
import com.coldstorage.oms.mapper.FaultCaseMapper;
import com.coldstorage.oms.mapper.FaultRecordMapper;
import com.coldstorage.oms.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FaultCaseService {

    @Autowired
    private FaultCaseMapper faultCaseMapper;
    @Autowired
    private FaultRecordMapper faultRecordMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private OperationLogService operationLogService;

    public Page<FaultCase> page(long page, long size, String deviceType, String faultType, String keyword) {
        requireStaff();
        LambdaQueryWrapper<FaultCase> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deviceType)) {
            qw.eq(FaultCase::getDeviceType, deviceType);
        }
        if (StringUtils.hasText(faultType)) {
            qw.eq(FaultCase::getFaultType, faultType);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(FaultCase::getTitle, keyword)
                    .or().like(FaultCase::getFaultPhenomenon, keyword)
                    .or().like(FaultCase::getFaultReason, keyword)
                    .or().like(FaultCase::getSolution, keyword)
                    .or().like(FaultCase::getCheckMethod, keyword));
        }
        qw.orderByDesc(FaultCase::getId);
        Page<FaultCase> result = faultCaseMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    public FaultCase detail(Long id) {
        requireStaff();
        FaultCase c = faultCaseMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("案例不存在");
        }
        fillExtra(Collections.singletonList(c));
        return c;
    }

    public void save(FaultCase form) {
        requireStaff();
        if (!StringUtils.hasText(form.getTitle()) || !StringUtils.hasText(form.getFaultType())) {
            throw new BusinessException("标题和故障类型不能为空");
        }
        if (form.getId() == null) {
            form.setCreatorId(UserContext.getUserId());
            faultCaseMapper.insert(form);
            operationLogService.log("故障案例", "新增", form.getTitle());
        } else {
            FaultCase db = faultCaseMapper.selectById(form.getId());
            if (db == null) {
                throw new BusinessException("案例不存在");
            }
            if (!"ADMIN".equals(UserContext.getRole())
                    && !UserContext.getUserId().equals(db.getCreatorId())) {
                throw new BusinessException("只能编辑自己创建的案例");
            }
            faultCaseMapper.updateById(form);
            operationLogService.log("故障案例", "编辑", form.getTitle());
        }
    }

    public void delete(Long id) {
        requireStaff();
        FaultCase db = faultCaseMapper.selectById(id);
        if (db == null) {
            return;
        }
        if (!"ADMIN".equals(UserContext.getRole())
                && !UserContext.getUserId().equals(db.getCreatorId())) {
            throw new BusinessException("只能删除自己创建的案例");
        }
        faultCaseMapper.deleteById(id);
        operationLogService.log("故障案例", "删除", db.getTitle());
    }

    /** 从故障记录沉淀为案例 */
    public Long promoteFromRecord(Long recordId, String title) {
        requireStaff();
        FaultRecord record = faultRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("故障记录不存在");
        }
        Device device = deviceMapper.selectById(record.getDeviceId());
        FaultCase c = new FaultCase();
        c.setTitle(StringUtils.hasText(title) ? title
                : (record.getFaultType() + "案例-" + record.getRecordNo()));
        c.setFaultPhenomenon(StringUtils.hasText(record.getFaultPhenomenon())
                ? record.getFaultPhenomenon() : record.getFaultDesc());
        c.setFaultType(record.getFaultType());
        c.setFaultReason(record.getFaultReason());
        c.setSolution(record.getSolution());
        c.setCheckMethod("参考现场处理过程与工单记录排查");
        c.setDeviceType(device == null ? null : device.getDeviceType());
        c.setNotice(record.getRemark());
        c.setAttachment(record.getFaultImages());
        c.setCreatorId(UserContext.getUserId());
        faultCaseMapper.insert(c);
        operationLogService.log("故障案例", "沉淀", c.getTitle());
        return c.getId();
    }

    private void fillExtra(List<FaultCase> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
        for (FaultCase c : list) {
            c.setCreatorName(userMap.get(c.getCreatorId()));
        }
    }

    private void requireStaff() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限访问故障案例库");
        }
    }
}
