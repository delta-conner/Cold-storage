package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("spare_part")
public class SparePart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String partNo;
    private String partName;
    private String partType;
    private String spec;
    private String brand;
    private String unit;
    private Integer stockQty;
    private Integer safetyStock;
    private Long supplierId;
    private BigDecimal unitPrice;
    private String remark;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private Boolean lowStock;
}
