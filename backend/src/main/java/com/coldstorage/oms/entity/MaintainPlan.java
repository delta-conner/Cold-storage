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
@TableName("maintain_plan")
public class MaintainPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String planNo;
    private String title;
    /** MONTHLY / QUARTERLY / HALF_YEAR / YEARLY */
    private String cycleType;
    private String deviceType;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date dueDate;
    private String status;
    private String remark;
    private Long creatorId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private List<String> items;
    @TableField(exist = false)
    private Integer taskTotal;
    @TableField(exist = false)
    private Integer taskDone;
    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private String displayStatus;
}
