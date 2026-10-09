package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("device_status_log")
public class DeviceStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    /** WORK_ORDER / MANUAL / SYSTEM */
    private String source;
    private Long operatorId;
    private String operatorName;
    private Date createTime;
}
