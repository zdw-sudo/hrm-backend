# 操作日志 AOP

基于 Spring AOP 的审计日志，对关键业务接口自动记录操作人、IP、参数、耗时与成功/失败状态。

## 设计

```
Controller 方法标注 @OperationLog
        ↓
OperationLogAspect @Around 拦截
        ↓
记录 module / action / operator / IP / params / duration / status
        ↓
异步落库 sys_operation_log（失败不影响主流程）
```

## 核心文件

| 文件 | 职责 |
|------|------|
| `annotation/OperationLog.java` | 自定义注解，`module` + `action` |
| `aspect/OperationLogAspect.java` | AOP 切面，SecurityContext 取操作人，密码脱敏 |
| `entity/OperationLogRecord.java` | 实体，表 `sys_operation_log` |
| `service/OperationLogService.java` | 保存与分页查询 |
| `controller/OperationLogController.java` | `GET /operation-log`，权限 `system:operation-log:list` |

## 已标注接口

| 模块 | 接口 | action |
|------|------|--------|
| 登录 | `POST /login/{validateCode}` | 用户登录 |
| 请假 | `POST /staff-leave/apply/{code}` | 提交申请 |
| 请假 | `POST /staff-leave/claim/{code}` | 拾取任务 |
| 请假 | `POST /staff-leave/complete/{code}` | 审批提交 |
| 请假 | `POST /staff-leave/cancel` | 撤销申请 |
| 考勤 | `POST /attendance/import` | 导入数据 |

## 初始化

```bash
mysql -u root -p hrm < src/main/resources/sql/operation-log.sql
```

执行后 **重新登录** admin，系统管理下会出现「操作日志」菜单。

## 前端页面

路径：`vue-elementui-hrm/src/views/system/operation-log/index.vue`  
路由由后端菜单动态生成：`/system/operation-log`（无需改 `router/index.js`）。

功能：模块/操作人筛选、分页列表、详情弹窗查看完整参数与方法签名。

## 查询示例

```http
GET /operation-log?current=1&size=10&module=请假&operator=admin
Authorization: Bearer {token}
```

## 面试要点

1. **为什么用 AOP**：业务与审计解耦，新增日志只需加注解，不改业务代码。
2. **登录场景**：登录时 SecurityContext 尚未写入，切面从 `Staff` 参数取工号作为 operator。
3. **敏感信息**：`Staff.password` 序列化前替换为 `******`，参数 JSON 超长截断至 2000 字符。
4. **失败不影响主流程**：`saveLog` 包在 try-catch，写库异常只打 warn 日志。
5. **成功判定**：返回值是 `ResponseDTO` 时，根据 `code` 是否为业务成功码写入 status。
