package com.qiujie.ai.scope;

/**
 * AI 工具查询的数据范围。
 */
public enum AiDataScope {

    /** 管理员 / 人事：全公司 */
    ALL,

    /** 部门负责人：本部门 */
    DEPT,

    /** 普通员工：仅本人 */
    SELF
}
