package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("cold_room")
public class ColdRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storageId;
    private String name;
    private String code;
    private String status;
    private BigDecimal tempMin;
    private BigDecimal tempMax;
    private BigDecimal volume;
    private String remark;
    private Date createTime;

    @TableField(exist = false)
    private String storageName;
}
