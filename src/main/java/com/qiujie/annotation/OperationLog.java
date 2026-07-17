package com.qiujie.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解，配合 OperationLogAspect 记录关键业务操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块名称，如：登录、请假、考勤 */
    String module();

    /** 操作类型，如：登录、审批、导入 */
    String action();
}
