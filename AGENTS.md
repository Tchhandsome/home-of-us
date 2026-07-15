# Home Of Us 项目协作指南

本文件补充全局规则，适用于 `home-of-us`。项目面向两人家庭的日常协作，优先保证移动端易用、家庭数据正确和长期可维护。

## 1. 技术栈与目录

- 采用模块化单体，不为架构形式拆分微服务。
- 后端位于 `backend`：Java 17、Spring Boot 2.7、Spring JDBC、MySQL 8、Flyway、Maven。
- 前端位于 `apps/mobile`、`apps/admin`：Vue 3、TypeScript、Vite；共享 API 位于 `apps/shared`，使用 npm workspaces。
- 数据库迁移位于 `backend/src/main/resources/db/migration`，上传文件默认位于 `data/uploads`。
- 需求与部署说明优先查看 `README.md`、`docs`、`docker-compose*.yml`。

## 2. 业务边界与不变量

- 快速记录必须保留原始文本；解析或结构化失败不能导致原始记录丢失。
- 提醒、附件和家庭成员上下文属于公共能力，业务模块应复用，不重复实现。
- 完成购物项时必须保存实际金额并生成一条支出记录；通过关联的账本记录 ID 保证重复操作不会重复入账。
- 私密空间、悄悄话等数据必须按当前成员隔离；共享数据保持家庭空间内一致可见。
- 图片上传继续使用配置化存储根目录和 `/api/uploads/**` 访问路径，不在代码中写死本机绝对路径。
- 修改跨模块联动时同时检查来源记录、派生数据、提醒、附件和删除行为。
- 当前已有花卉、宠物、家务、购物、库存、账本、相册、菜谱、周期记录、私密空间和家庭投票等模块，不按旧 V1 清单删除现有能力。

## 3. 后端规范

- 按领域包组织 `controller`、`service`、`repository`、`dto` 等职责；业务逻辑不堆入 Controller。
- 新增和修改的业务代码使用 `Objects.isNull()`、`Objects.nonNull()`；字符串使用 `StringUtils`，不批量改写未涉及旧代码。
- 需要访问器或构造器时优先使用 Lombok 并遵循相邻代码风格，不默认使用 `@Data`。
- 业务异常保留稳定错误码和当前消息兼容性；项目尚未形成完整 I18n 基础设施，不在单个需求中引入半套国际化方案。
- 不吞掉异常，不记录密码、令牌、完整请求体或家庭私密内容。
- 新增类型和关键业务方法使用中文 JavaDoc；新建类型使用 `@author tanchaohong`。
- 金额使用 `BigDecimal`/数据库 `DECIMAL`，时间处理复用项目的时间抽象，业务枚举保存稳定编码。
- Flyway 迁移只新增版本脚本，不修改已执行的历史迁移；同步考虑初始化数据和旧数据兼容。

## 4. 前端规范

- 移动端优先减少输入、跳转和操作层级，保证单手操作和常用入口可达。
- 管理端侧重批量整理、筛选、统计和档案维护；移动端与管理端复用 `apps/shared` 的 API 定义，不复制协议模型。
- Vue 组件保持职责单一；共享业务请求和类型放入 `apps/shared`，端侧视觉与交互留在各自应用。
- 修改 API 字段时同时检查后端 DTO、共享 API、移动端和管理端调用方。
- 不直接编辑构建产物、上传目录或打包归档文件。

## 5. 最小构建与测试

后端编译与单测：

```bash
mvn -f backend/pom.xml -DskipTests compile
mvn -f backend/pom.xml -Dtest=TestClassName test
mvn -f backend/pom.xml -Dtest=TestClassName#testMethod test
```

前端按影响范围构建：

```bash
npm run build:mobile
npm run build:admin
npm run build
```

- 后端业务逻辑修改优先执行对应单测试类或方法；跨领域公共逻辑必要时执行后端模块测试。
- 修改 `apps/shared` 或同时影响两个端时执行根目录 `npm run build`；只影响单端时执行对应构建。
- 数据库或运行联调需要 MySQL 时使用 `docker compose up -d mysql`；未明确要求不启动完整生产编排、不构建发布镜像。

## 6. 交付检查

- 业务不变量、成员数据隔离和跨模块联动未被破坏。
- 后端修改完成 Maven 编译及相关最小测试，前端修改通过对应 TypeScript/Vite 构建。
- 数据库迁移、API 变更、上传路径和部署配置保持兼容。
- 最终答复列出修改范围、验证结果、未验证事项和剩余风险。

