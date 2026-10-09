package com.coldstorage.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coldstorage.oms.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
