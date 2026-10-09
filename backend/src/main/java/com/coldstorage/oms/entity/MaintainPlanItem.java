package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("maintain_plan_item")
public class MaintainPlanItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String itemName;
    private Integer sortNo;
}
