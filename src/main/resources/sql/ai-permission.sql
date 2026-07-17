-- AI 助手权限：仅管理员(admin)与人事经理(hr)可用
-- 执行后请让相关用户重新登录，JWT 中的权限才会刷新

INSERT INTO `per_menu` (`id`, `code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 102, 'ai', '智能助手', 'chat-dot-round', 'ai:chat', 5, 1, 1, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `per_menu` WHERE `id` = 102 OR `permission` = 'ai:chat');

INSERT INTO `per_role_menu` (`id`, `role_id`, `menu_id`)
SELECT 3764, 1, 102
WHERE NOT EXISTS (SELECT 1 FROM `per_role_menu` WHERE `role_id` = 1 AND `menu_id` = 102);

INSERT INTO `per_role_menu` (`id`, `role_id`, `menu_id`)
SELECT 3765, 7, 102
WHERE NOT EXISTS (SELECT 1 FROM `per_role_menu` WHERE `role_id` = 7 AND `menu_id` = 102);
