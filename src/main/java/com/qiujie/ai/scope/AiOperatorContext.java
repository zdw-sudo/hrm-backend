package com.qiujie.ai.scope;

import lombok.Data;

/**
 * 当前 AI 对话操作人上下文。
 */
@Data
public class AiOperatorContext {

    private Integer staffId;
    private String code;
    private String name;
    private Integer deptId;
    private String deptName;
    private AiDataScope scope;
}
