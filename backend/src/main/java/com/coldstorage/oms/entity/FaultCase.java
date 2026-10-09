package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("fault_case")
public class FaultCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String faultPhenomenon;
    private String faultType;
    private String faultReason;
    private String checkMethod;
    private String solution;
    private String deviceType;
    private String notice;
    private String attachment;
    private Long creatorId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String creatorName;
}
