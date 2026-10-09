package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("work_order_part")
public class WorkOrderPart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long partId;
    private Integer quantity;
    private Date createTime;

    @TableField(exist = false)
    private String partNo;
    @TableField(exist = false)
    private String partName;
    @TableField(exist = false)
    private String unit;
}
