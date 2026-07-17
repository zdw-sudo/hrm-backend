-- 操作审计日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module`       VARCHAR(50)  DEFAULT NULL COMMENT '模块',
    `action`       VARCHAR(50)  DEFAULT NULL COMMENT '操作',
    `method`       VARCHAR(200) DEFAULT NULL COMMENT '方法签名',
    `request_uri`  VARCHAR(200) DEFAULT NULL COMMENT '请求路径',
    `operator`     VARCHAR(50)  DEFAULT NULL COMMENT '操作人工号',
    `operator_ip`  VARCHAR(50)  DEFAULT NULL COMMENT '操作 IP',
    `params`       TEXT         COMMENT '请求参数',
    `duration`     BIGINT       DEFAULT NULL COMMENT '耗时(ms)',
    `status`       TINYINT      DEFAULT 1 COMMENT '1成功 0失败',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator`),
    KEY `idx_module` (`module`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- 操作日志查询权限（仅管理员，执行后请重新登录）
INSERT INTO `per_menu` (`id`, `code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 103, 'operation-log', '操作日志', 'document', 'system:operation-log:list', 5, 1, 1, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `per_menu` WHERE `id` = 103 OR `permission` = 'system:operation-log:list');

INSERT INTO `per_role_menu` (`id`, `role_id`, `menu_id`)
SELECT 3766, 1, 103
WHERE NOT EXISTS (SELECT 1 FROM `per_role_menu` WHERE `role_id` = 1 AND `menu_id` = 103);
