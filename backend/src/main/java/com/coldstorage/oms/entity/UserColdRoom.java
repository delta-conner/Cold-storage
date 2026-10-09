package com.coldstorage.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_cold_room")
public class UserColdRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roomId;
}
