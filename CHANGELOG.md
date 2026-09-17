# 更新日志

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循
[语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 新增

- `scripts/check-versions.py`：版本号散在 10 处，一条命令检查或批量改全（发版第一步用它）
- 构建产物补齐 `-javadoc.jar` 附件（此前只有 `-sources.jar`），并新增
  `gradle publishNotifyHubToCentralBundle`，产出可直接上传 Maven Central 的 zip
- CI：新增「Maven Central 上传包」「示例工程」两个 job，以及版本号、许可证副本的一致性校验

### 变更

- GitHub Packages 的 Maven 发布改为幂等：版本已存在就跳过，不再因 409 让 Release 变红

### 修复

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
