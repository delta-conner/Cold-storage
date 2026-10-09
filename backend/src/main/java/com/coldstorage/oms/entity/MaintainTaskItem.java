package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("maintain_task_item")
public class MaintainTaskItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String itemName;
    /** OK / ABNORMAL / NA */
    private String result;
    private String remark;
    private Integer sortNo;
}
