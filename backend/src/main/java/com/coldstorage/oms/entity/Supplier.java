package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String contactName;
    private String phone;
    private String address;
    private String supplyScope;
    /** COOPERATING / STOPPED */
    private String status;
    private String remark;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private Integer partCount;
}
