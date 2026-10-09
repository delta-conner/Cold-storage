package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("maintain_task")
public class MaintainTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long planId;
    private Long deviceId;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date dueDate;
    private String status;
    private Long assigneeId;
    private String resultSummary;
    private Integer hasAbnormal;
    private String measures;
    private String partsUsed;
    private Date finishTime;
    private Long handlerId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String planTitle;
    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String deviceNo;
    @TableField(exist = false)
    private String deviceType;
    @TableField(exist = false)
    private String roomName;
    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private String handlerName;
    @TableField(exist = false)
    private String displayStatus;
    @TableField(exist = false)
    private Long daysLeft;
    @TableField(exist = false)
    private List<MaintainTaskItem> items;
}
