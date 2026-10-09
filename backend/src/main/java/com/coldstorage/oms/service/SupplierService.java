package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.SparePart;
import com.coldstorage.oms.entity.Supplier;
import com.coldstorage.oms.mapper.SparePartMapper;
import com.coldstorage.oms.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SupplierService {

    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private SparePartMapper sparePartMapper;
    @Autowired
    private OperationLogService operationLogService;

    public Page<Supplier> page(long page, long size, String name, String status) {
        requireStaff();
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            qw.like(Supplier::getName, name);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Supplier::getStatus, status);
        }
        qw.orderByDesc(Supplier::getId);
        Page<Supplier> result = supplierMapper.selectPage(new Page<>(page, size), qw);
        for (Supplier s : result.getRecords()) {
            Long cnt = sparePartMapper.selectCount(new LambdaQueryWrapper<SparePart>()
                    .eq(SparePart::getSupplierId, s.getId()));
            s.setPartCount(cnt == null ? 0 : cnt.intValue());
        }
        return result;
    }

    public List<Supplier> listActive() {
        requireStaff();
        return supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getStatus, "COOPERATING")
                .orderByAsc(Supplier::getId));
    }

    public Supplier detail(Long id) {
        requireStaff();
        Supplier s = supplierMapper.selectById(id);
        if (s == null) {
            throw new BusinessException("供应商不存在");
        }
        Long cnt = sparePartMapper.selectCount(new LambdaQueryWrapper<SparePart>()
                .eq(SparePart::getSupplierId, id));
        s.setPartCount(cnt == null ? 0 : cnt.intValue());
        return s;
    }

    public void save(Supplier form) {
        requireAdmin();
        if (!StringUtils.hasText(form.getName())) {
            throw new BusinessException("供应商名称不能为空");
        }
        if (!StringUtils.hasText(form.getStatus())) {
            form.setStatus("COOPERATING");
        }
        if (form.getId() == null) {
            supplierMapper.insert(form);
            operationLogService.log("供应商", "新增", form.getName());
        } else {
            supplierMapper.updateById(form);
            operationLogService.log("供应商", "编辑", form.getName());
        }
    }

    public void delete(Long id) {
        requireAdmin();
        Long cnt = sparePartMapper.selectCount(new LambdaQueryWrapper<SparePart>()
                .eq(SparePart::getSupplierId, id));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("该供应商仍有关联备件，不能删除");
        }
        supplierMapper.deleteById(id);
        operationLogService.log("供应商", "删除", "ID：" + id);
    }

    private void requireStaff() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"OPS".equals(role)) {
            throw new BusinessException("无权限");
        }
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException("仅管理员可维护供应商");
        }
    }
}
