package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("fault_record")
public class FaultRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private Long deviceId;
    private Long orderId;
    private String faultType;
    /** 一般 / 严重 / 紧急 */
    private String faultLevel;
    private String faultDesc;
    private String faultPhenomenon;
    private String faultReason;
    private String solution;
    private Date startTime;
    private Date endTime;
    private Integer durationMinutes;
    private String partsUsed;
    private String faultImages;
    private String remark;
    private Long handlerId;
    private Date handleTime;
    private Date createTime;

    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String deviceNo;
    @TableField(exist = false)
    private String deviceType;
    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String handlerName;
    @TableField(exist = false)
    private String roomName;
}
