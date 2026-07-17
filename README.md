# 人力资源管理系统（HRM）

面向企业人事场景的后台管理系统，覆盖员工档案、考勤、请假审批、薪资等模块。  
本人在开源项目基础上完成 **JWT 鉴权、Activiti 工作流、Spring AI 智能助手** 等模块的开发与扩展。

> 前端仓库：`../vue-elementui-hrm`（Vue 2 + Element UI）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3、Spring Security、JWT |
| 工作流 | Activiti 8 |
| 持久层 | MyBatis-Plus、MySQL（双数据源） |
| 缓存 | Redis（验证码、AI 对话历史） |
| AI | Spring AI 1.0 + DeepSeek + Tool Calling |
| 文档 | SpringDoc OpenAPI 3 |
| 前端 | Vue 2、Element UI、Axios |

**运行环境：** JDK 17、MySQL 8、Redis 5+、Node.js 16+

---

## 功能模块

| 模块 | 说明 |
|------|------|
| 权限管理 | RBAC 四表模型，JWT 无状态登录，`@PreAuthorize` 接口权限 |
| 考勤管理 | 考勤导入/导出、规则计算、月报表 |
| 请假审批 | Activiti 多级审批，审批结果同步考勤 |
| 财务管理 | 薪资、五险一金、加班 |
| 智能助手 | 自然语言查询员工/考勤/请假（Spring AI Tool） |

---

## 核心亮点（面试可讲）

### 1. JWT 无状态鉴权

```
登录 → 验证码(Redis) → AuthenticationManager 认证 → 签发 JWT
请求 → JwtAuthenticationFilter 解析 Token → 加载 RBAC 权限 → @PreAuthorize 校验
未登录 401 / 无权限 403
```

详见：[docs/鉴权流程.md](docs/鉴权流程.md)

**核心代码：**

| 文件 | 职责 |
|------|------|
| `filter/JwtAuthenticationFilter.java` | 解析 Token，写入 SecurityContext |
| `config/SecurityConfig.java` | 白名单、无 Session、Filter 链 |
| `service/StaffDetailsService.java` | 加载用户与 RBAC 权限 |
| `service/StaffLeaveService.java` | 请假业务 + Activiti 交互 |

### 2. Activiti 请假工作流

```
员工提交 → HR 审批 → 经理审批
businessKey 关联 staff_leave 表
经理通过后 ExecutionListener 同步请假日至考勤表
业务库(hrm) 与 流程库(hrm_activiti) 双数据源分离
```

详见：[docs/请假流程.md](docs/请假流程.md)

### 3. Spring AI 智能助手（本人扩展）

- Tool Calling 对接员工查询、月考勤统计、请假记录
- Redis 多轮对话 + SSE 流式输出
- 权限 `ai:chat`（admin / HR 经理）

详见：[docs/AI助手说明.md](docs/AI助手说明.md)

---

## 项目结构

```
hrm/
├── src/main/java/com/qiujie/
│   ├── filter/          # JWT 过滤器
│   ├── config/          # Security、双数据源、Redis、AI
│   ├── service/         # 业务逻辑
│   ├── controller/      # REST 接口
│   ├── listener/        # Activiti 监听器
│   └── ai/              # Spring AI 模块
│       ├── config/
│       ├── controller/
│       ├── service/
│       └── tool/        # HrQueryTools
├── src/main/resources/
│   ├── processes/leave.bpmn20.xml
│   └── sql/ai-permission.sql
└── docs/                # 说明文档
```

---

## 快速启动

### 1. 准备环境

- JDK 17
- MySQL 8
- Redis
- Maven 3.8+
- Node.js（前端）

### 2. 初始化数据库

```bash
# 在 MySQL 中执行（SQL 文件在项目上级目录）
mysql -u root -p < ../hrm.sql
mysql -u root -p < ../hrm_activiti.sql

# AI 助手权限（可选，启用 AI 时执行）
mysql -u root -p hrm < src/main/resources/sql/ai-permission.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    master:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/hrm?...
      username: root
      password: 你的密码
    activiti:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/hrm_activiti?...
      username: root
      password: 你的密码
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 你的密码
```

**AI 模块 Key（不要写进 application.yml）：**

```bash
# 复制本地配置模板
copy src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml，填入 DeepSeek API Key
```

IDEA 启动配置：**Active profiles** 填 `local`  
（`application-local.yml` 已在 `.gitignore`，不会提交）

若不需要 AI，可关闭：

```yaml
hrm:
  ai:
    enabled: false
```

### 4. 启动后端

```bash
cd hrm
mvn spring-boot:run
# 或在 IDEA 中运行 HrmApplication，profile=local
```

默认端口：**8888**  
接口文档：http://localhost:8888/swagger-ui.html

### 5. 启动前端

```bash
cd ../vue-elementui-hrm
npm install
npm run serve
```

前端默认：http://localhost:8080  
`.env` 中 `VUE_APP_PORT = 8888` 需与后端端口一致。

### 6. 登录测试

| 项 | 值 |
|----|-----|
| 地址 | http://localhost:8080/login |
| 账号 | admin |
| 密码 | 123 |

> 使用 AI 助手需 admin 或 HR 经理角色，执行 `ai-permission.sql` 后重新登录。

---

## 接口示例

### 登录

```http
POST /login/{validateCode}
Content-Type: application/json

{ "code": "admin", "password": "123" }
```

### AI 对话（需 Token + ai:chat 权限）

```http
POST /ai/chat
Authorization: Bearer {token}
Content-Type: application/json

{ "message": "帮我查一下张三在哪个部门" }
```

---

## 截图（可自行补充到 docs/screenshots/）

| 功能 | 路径 |
|------|------|
| 登录页 | `docs/screenshots/login.png` |
| 请假审批 | `docs/screenshots/leave.png` |
| 智能助手 | `docs/screenshots/ai-chat.png` |

---

## 个人负责部分

- [x] Spring Security + JWT 全链路鉴权与 RBAC
- [x] Activiti 请假流程配置与 ExecutionListener 考勤联动
- [x] 双数据源配置（业务库 + Activiti 库）
- [x] Spring AI 智能助手（Tool Calling + Redis 多轮 + SSE）
- [x] API Key 外部化配置（环境变量 / application-local.yml）

---

## 常见问题

**Q：启动报 `OpenAI API key must be set`？**  
A：复制 `application-local.yml.example` 为 `application-local.yml` 填入 Key，IDEA Active profiles 设 `local`；或设 `hrm.ai.enabled=false`。

**Q：智能助手菜单看不到？**  
A：执行 `sql/ai-permission.sql`，重新登录刷新 JWT 权限。

**Q：Activiti 依赖下载慢？**  
A：`pom.xml` 已配置 Alfresco 仓库，首次构建需联网等待。

---

## 说明

本项目基于开源 HRM 项目学习与二次开发，上述「个人负责部分」为本人实现、调试并可完整讲解的模块。

---

## 相关文档

- [JWT 鉴权流程](docs/鉴权流程.md)
- [Activiti 请假流程](docs/请假流程.md)
- [AI 智能助手](docs/AI助手说明.md)
