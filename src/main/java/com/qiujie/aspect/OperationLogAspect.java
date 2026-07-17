package com.qiujie.aspect;

import com.alibaba.fastjson.JSON;
import com.qiujie.annotation.OperationLog;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.OperationLogRecord;
import com.qiujie.entity.Staff;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Order(1)
public class OperationLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_PARAM_LENGTH = 2000;

    @Autowired
    private OperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogRecord record = new OperationLogRecord();
        record.setModule(operationLog.module());
        record.setAction(operationLog.action());
        record.setMethod(joinPoint.getSignature().toShortString());
        record.setCreateTime(new Timestamp(System.currentTimeMillis()));

        HttpServletRequest request = currentRequest();
        if (request != null) {
            record.setRequestUri(request.getRequestURI());
            record.setOperatorIp(resolveClientIp(request));
        }
        record.setParams(buildParams(joinPoint.getArgs()));

        Object result = null;
        int status = 1;
        try {
            result = joinPoint.proceed();
            status = resolveStatus(result);
            return result;
        } catch (Throwable ex) {
            status = 0;
            throw ex;
        } finally {
            record.setOperator(resolveOperator(joinPoint.getArgs()));
            record.setStatus(status);
            record.setDuration(System.currentTimeMillis() - start);
            try {
                operationLogService.saveLog(record);
            } catch (Exception ex) {
                logger.warn("保存操作日志失败: {}", ex.getMessage());
            }
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String resolveOperator(Object[] args) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return authentication.getName();
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Staff staff && staff.getCode() != null && !staff.getCode().isBlank()) {
                    return staff.getCode();
                }
            }
        }
        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        List<Object> safeArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                continue;
            }
            if (arg instanceof MultipartFile file) {
                safeArgs.add("MultipartFile:" + file.getOriginalFilename());
                continue;
            }
            if (arg instanceof Staff staff) {
                Staff copy = new Staff();
                copy.setCode(staff.getCode());
                copy.setName(staff.getName());
                copy.setPassword(staff.getPassword() != null ? "******" : null);
                safeArgs.add(copy);
                continue;
            }
            safeArgs.add(arg);
        }
        String json = JSON.toJSONString(safeArgs);
        return json.length() > MAX_PARAM_LENGTH ? json.substring(0, MAX_PARAM_LENGTH) + "..." : json;
    }

    private int resolveStatus(Object result) {
        if (result instanceof ResponseDTO responseDTO) {
            return responseDTO.getCode() == BusinessStatusEnum.SUCCESS.getCode() ? 1 : 0;
        }
        return 1;
    }
}
