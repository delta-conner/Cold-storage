package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("stock_record")
public class StockRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private Long partId;
    /** IN / OUT / ADJUST / CHECK */
    private String changeType;
    private Integer changeQty;
    private Integer beforeQty;
    private Integer afterQty;
    private String bizType;
    private Long bizId;
    private String remark;
    private Long operatorId;
    private Date createTime;

    @TableField(exist = false)
    private String partName;
    @TableField(exist = false)
    private String partNo;
    @TableField(exist = false)
    private String operatorName;
}
