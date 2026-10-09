package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceNo;
    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private Long roomId;
    private Integer isPublic;
    /** NORMAL/FAULT/REPAIRING/ACCEPTING/SCRAPPED */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date installDate;
    private Integer maintainCycleDays;
    private Long ownerOpsId;
    private String ratedParams;
    private Integer faultCount;
    private String remark;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String roomName;
    @TableField(exist = false)
    private String storageName;
    @TableField(exist = false)
    private Long storageId;
    @TableField(exist = false)
    private String ownerOpsName;
}
