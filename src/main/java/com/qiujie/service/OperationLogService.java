package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.OperationLogRecord;
import com.qiujie.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLogRecord> {

    public void saveLog(OperationLogRecord record) {
        this.save(record);
    }

    public ResponseDTO list(Integer current, Integer size, String module, String operator) {
        Page<OperationLogRecord> page = new Page<>(current, size);
        QueryWrapper<OperationLogRecord> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.like("module", module);
        }
        if (StringUtils.hasText(operator)) {
            wrapper.like("operator", operator);
        }
        wrapper.orderByDesc("create_time");
        IPage<OperationLogRecord> result = this.page(page, wrapper);
        Map<String, Object> data = new HashMap<>(2);
        data.put("total", result.getTotal());
        data.put("list", result.getRecords());
        return Response.success(data);
    }
}
