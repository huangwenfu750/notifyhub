# 更新日志

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循
[语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 新增

- 文档：Maven Central 发布补上 GPG 密钥的完整操作步骤（只生成主密钥 → 传公钥到 keyserver →
  导出私钥填 Secret），并列出各平台发布所需的 Secrets 一览表
- `.gitignore` 排除 `*.asc`，防止导出的 GPG 私钥被误提交

## [0.1.1] - 2026-09-17

### 新增

- `scripts/check-versions.py`：版本号散在 10 处，一条命令检查或批量改全（发版第一步用它）
- 构建产物补齐 `-javadoc.jar` 附件（此前只有 `-sources.jar`），并新增
  `gradle publishNotifyHubToCentralBundle`，产出可直接上传 Maven Central 的 zip
- CI：新增「Maven Central 上传包」「示例工程」「Go 模块标签」三个 job，以及版本号、许可证副本的
  一致性校验。Go 模块标签 `sdks/go/v*` 从此自动打，不再依赖手工

### 变更

- GitHub Packages 的 Maven 发布改为幂等：版本已存在就跳过，不再因 409 让 Release 变红
- Go SDK 的 `gen/` gRPC stub 改为随仓库发布（原先被 `.gitignore` 排除），`go get` 后直接可编译，
  不再要求使用者本地跑 protoc

### 修复

- **Go SDK v0.1.0 完全不可用**：`.gitignore` 里的 `sdks/go/gen/` 让发布的模块包缺少
  `gen/notify/v1`，`go get` 后必定报 `cannot find module providing package ...`。生成物已入库，
  请改用 v0.1.1 —— Go 模块代理按版本号永久缓存，v0.1.0 无法原地修复
- npm / PyPI 发布包此前只有许可证元数据、没有 LICENSE 文件，现已随包分发
- 文档：明确 `io.github.huangwenfu750:*` 只发在 GitHub Packages、不在 Maven Central，
  并补齐 Maven `settings.xml` 与 Gradle 两种取包配置（原先只有根目录 README 有 Gradle 示例）

## [0.1.0] - 2026-09-16

首个版本。

### 新增

- gRPC 服务端：topic 通配路由、模板渲染、去重、死信、限流，Admin RPC 可在运行时管理推送平台
- 投递渠道：钉钉、企业微信、飞书、通用 Webhook（各自签名算法见 `docs/protocol.md`）
- 四语言 SDK：Java / Kotlin、Python、TypeScript / JavaScript、Go
- Spring Boot Starter：自动装配 `NotifyClient` 与 `NotifyHubTemplate`，含健康检查与 Micrometer 埋点
- 分发方式：Linux 发行包（自带 JRE / 精简版，各带 `.sha256`）、Docker 镜像、systemd 服务单元

### 说明

- 投递语义是**至多一次、不持久化、不重放**：进程重启后未投递的消息不会补发，
  需要可靠投递请在业务侧自行重投。
