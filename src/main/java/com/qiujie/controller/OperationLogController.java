package com.qiujie.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation-log")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    @PreAuthorize("hasAuthority('system:operation-log:list')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String module,
                            String operator) {
        return operationLogService.list(current, size, module, operator);
    }
}
