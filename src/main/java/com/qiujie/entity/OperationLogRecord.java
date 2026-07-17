package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("sys_operation_log")
public class OperationLogRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("module")
    private String module;

    @TableField("action")
    private String action;

    @TableField("method")
    private String method;

    @TableField("request_uri")
    private String requestUri;

    @TableField("operator")
    private String operator;

    @TableField("operator_ip")
    private String operatorIp;

    @TableField("params")
    private String params;

    @TableField("duration")
    private Long duration;

    /** 1 成功 0 失败 */
    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private Timestamp createTime;
}
