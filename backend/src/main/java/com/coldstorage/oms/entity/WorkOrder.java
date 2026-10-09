package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("work_order")
public class WorkOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long deviceId;
    private String faultDesc;
    /** 报修图片路径，逗号分隔 */
    private String faultImages;
    private String status;
    private Long submitUserId;
    private Long assigneeId;
    private String processRecord;
    private String faultType;
    private String faultReason;
    private String solution;
    private Integer durationMinutes;
    private String repairImages;
    private Integer satisfaction;
    private String evaluateContent;
    private String acceptRemark;
    private String closeReason;
    private Date assignTime;
    private Date finishTime;
    private Date acceptTime;
    private Date evaluateTime;
    private Date archiveTime;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String deviceNo;
    @TableField(exist = false)
    private String roomName;
    @TableField(exist = false)
    private String storageName;
    @TableField(exist = false)
    private String submitUserName;
    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private java.util.List<WorkOrderPart> parts;
}
