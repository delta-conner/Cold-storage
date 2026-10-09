package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.common.UserContext;
import com.coldstorage.oms.entity.SparePart;
import com.coldstorage.oms.entity.StockRecord;
import com.coldstorage.oms.entity.Supplier;
import com.coldstorage.oms.entity.SysUser;
import com.coldstorage.oms.mapper.SparePartMapper;
import com.coldstorage.oms.mapper.StockRecordMapper;
import com.coldstorage.oms.mapper.SupplierMapper;
import com.coldstorage.oms.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SparePartService {

    @Autowired
    private SparePartMapper sparePartMapper;
    @Autowired
    private StockRecordMapper stockRecordMapper;
    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private MessageService messageService;

    public Page<SparePart> page(long page, long size, String partNo, String partName,
                                String partType, Long supplierId, Boolean lowOnly) {
        requireStaff();
        LambdaQueryWrapper<SparePart> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(partNo)) {
            qw.like(SparePart::getPartNo, partNo);
        }
        if (StringUtils.hasText(partName)) {
            qw.like(SparePart::getPartName, partName);
        }
        if (StringUtils.hasText(partType)) {
            qw.eq(SparePart::getPartType, partType);
        }
        if (supplierId != null) {
            qw.eq(SparePart::getSupplierId, supplierId);
        }
        qw.orderByDesc(SparePart::getId);
        Page<SparePart> result = sparePartMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        if (Boolean.TRUE.equals(lowOnly)) {
            List<SparePart> low = result.getRecords().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getLowStock()))
                    .collect(Collectors.toList());
            result.setRecords(low);
            result.setTotal(low.size());
        }
        return result;
    }

    public List<SparePart> listForSelect() {
        requireStaff();
        List<SparePart> list = sparePartMapper.selectList(new LambdaQueryWrapper<SparePart>()
                .orderByAsc(SparePart::getPartNo));
        fillExtra(list);
        return list;
    }

    public SparePart detail(Long id) {
        requireStaff();
        SparePart p = sparePartMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("备件不存在");
        }
        fillExtra(Collections.singletonList(p));
        return p;
    }

    public void save(SparePart form) {
        requireAdmin();
        if (!StringUtils.hasText(form.getPartNo()) || !StringUtils.hasText(form.getPartName())) {
            throw new BusinessException("备件编号和名称不能为空");
        }
        if (!StringUtils.hasText(form.getUnit())) {
            form.setUnit("个");
        }
        if (form.getStockQty() == null) {
            form.setStockQty(0);
        }
        if (form.getSafetyStock() == null) {
            form.setSafetyStock(0);
        }
        SparePart exists = sparePartMapper.selectOne(new LambdaQueryWrapper<SparePart>()
                .eq(SparePart::getPartNo, form.getPartNo())
                .ne(form.getId() != null, SparePart::getId, form.getId()));
        if (exists != null) {
            throw new BusinessException("备件编号已存在");
        }
        if (form.getId() == null) {
            int initQty = form.getStockQty();
            form.setStockQty(0);
            sparePartMapper.insert(form);
            if (initQty > 0) {
                changeStock(form.getId(), "IN", initQty, "MANUAL", null, "建档初始入库");
            }
            operationLogService.log("备件管理", "新增", form.getPartNo());
        } else {
            SparePart db = sparePartMapper.selectById(form.getId());
            if (db == null) {
                throw new BusinessException("备件不存在");
            }
            // 编辑不直接改库存数量，走出入库
            form.setStockQty(db.getStockQty());
            sparePartMapper.updateById(form);
            operationLogService.log("备件管理", "编辑", form.getPartNo());
        }
    }

    public void delete(Long id) {
        requireAdmin();
        SparePart p = sparePartMapper.selectById(id);
        if (p == null) {
            return;
        }
        if (p.getStockQty() != null && p.getStockQty() > 0) {
            throw new BusinessException("仍有库存，不能删除");
        }
        sparePartMapper.deleteById(id);
        operationLogService.log("备件管理", "删除", p.getPartNo());
    }

    /**
     * 库存变动：IN 入库 / OUT 出库 / ADJUST 调整到目标数量 / CHECK 盘点（同调整）
     */
    @Transactional
    public void changeStock(Long partId, String changeType, Integer qtyOrTarget,
                            String bizType, Long bizId, String remark) {
        requireStaff();
        if (partId == null || !StringUtils.hasText(changeType)) {
            throw new BusinessException("备件和变动类型不能为空");
        }
        SparePart part = sparePartMapper.selectById(partId);
        if (part == null) {
            throw new BusinessException("备件不存在");
        }
        int before = part.getStockQty() == null ? 0 : part.getStockQty();
        int after;
        int delta;
        String type = changeType.toUpperCase();
        if ("IN".equals(type)) {
            if (qtyOrTarget == null || qtyOrTarget <= 0) {
                throw new BusinessException("入库数量必须大于0");
            }
            delta = qtyOrTarget;
            after = before + delta;
        } else if ("OUT".equals(type)) {
            if (qtyOrTarget == null || qtyOrTarget <= 0) {
                throw new BusinessException("出库数量必须大于0");
            }
            if (before < qtyOrTarget) {
                throw new BusinessException("库存不足：" + part.getPartName() + "（当前" + before + "）");
            }
            delta = -qtyOrTarget;
            after = before + delta;
        } else if ("ADJUST".equals(type) || "CHECK".equals(type)) {
            if (qtyOrTarget == null || qtyOrTarget < 0) {
                throw new BusinessException("调整后库存不能为负");
            }
            after = qtyOrTarget;
            delta = after - before;
            if (delta == 0) {
                throw new BusinessException("库存未变化");
            }
        } else {
            throw new BusinessException("不支持的变动类型");
        }

        part.setStockQty(after);
        sparePartMapper.updateById(part);

        StockRecord rec = new StockRecord();
        rec.setRecordNo(genRecordNo());
        rec.setPartId(partId);
        rec.setChangeType(type);
        rec.setChangeQty(delta);
        rec.setBeforeQty(before);
        rec.setAfterQty(after);
        rec.setBizType(StringUtils.hasText(bizType) ? bizType : "MANUAL");
        rec.setBizId(bizId);
        rec.setRemark(remark);
        rec.setOperatorId(UserContext.getUserId());
        stockRecordMapper.insert(rec);
        operationLogService.log("库存", type, part.getPartNo() + " " + before + "→" + after);
        int safety = part.getSafetyStock() == null ? 0 : part.getSafetyStock();
        if (after < safety) {
            try {
                messageService.sendToAdmins("库存不足预警",
                        "备件 " + part.getPartName() + "（" + part.getPartNo() + "）当前库存 " + after
                                + "，低于安全库存 " + safety,
                        "STOCK_LOW", "SPARE_PART", part.getId());
            } catch (Exception ignored) {
                // 消息失败不影响库存
            }
        }
    }

    /**
     * 业务领用出库（工单/维保），已存在相同 biz 领用不重复扣（按 order parts 表控制）
     */
    @Transactional
    public void consumeOut(Long partId, int qty, String bizType, Long bizId, String remark) {
        changeStock(partId, "OUT", qty, bizType, bizId, remark);
    }

    public Page<StockRecord> recordPage(long page, long size, Long partId, String changeType) {
        requireStaff();
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        if (partId != null) {
            qw.eq(StockRecord::getPartId, partId);
        }
        if (StringUtils.hasText(changeType)) {
            qw.eq(StockRecord::getChangeType, changeType);
        }
        qw.orderByDesc(StockRecord::getId);
        Page<StockRecord> result = stockRecordMapper.selectPage(new Page<>(page, size), qw);
        fillRecordExtra(result.getRecords());
        return result;
    }

    public Map<String, Object> stockSummary() {
        requireStaff();
        List<SparePart> all = sparePartMapper.selectList(null);
        fillExtra(all);
        long totalSku = all.size();
        long low = all.stream().filter(p -> Boolean.TRUE.equals(p.getLowStock())).count();
        int totalQty = all.stream().mapToInt(p -> p.getStockQty() == null ? 0 : p.getStockQty()).sum();
        Map<String, Object> map = new HashMap<>();
        map.put("skuCount", totalSku);
        map.put("lowStockCount", low);
        map.put("totalQty", totalQty);
        map.put("lowParts", all.stream().filter(p -> Boolean.TRUE.equals(p.getLowStock())).limit(10).collect(Collectors.toList()));
        return map;
    }

    private void fillExtra(List<SparePart> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> supplierMap = supplierMapper.selectList(null).stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName, (a, b) -> a));
        for (SparePart p : list) {
            p.setSupplierName(supplierMap.get(p.getSupplierId()));
            int stock = p.getStockQty() == null ? 0 : p.getStockQty();
            int safety = p.getSafetyStock() == null ? 0 : p.getSafetyStock();
            p.setLowStock(stock < safety);
        }
    }

    private void fillRecordExtra(List<StockRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, SparePart> partMap = sparePartMapper.selectList(null).stream()
                .collect(Collectors.toMap(SparePart::getId, p -> p, (a, b) -> a));
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
        for (StockRecord r : list) {
            SparePart p = partMap.get(r.getPartId());
            if (p != null) {
                r.setPartName(p.getPartName());
                r.setPartNo(p.getPartNo());
            }
            r.setOperatorName(userMap.get(r.getOperatorId()));
        }
    }

    private String genRecordNo() {
        return "ST" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
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
            throw new BusinessException("仅管理员可维护备件档案");
        }
    }
}
