# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

xxl-job-alarm 是一个 Spring Boot 自动配置扩展库，用于扩展 XXL-Job Admin 的告警能力。XXL-Job 原生仅支持邮件告警，本库新增了：
- 邮件告警的开关控制（`xxl-job.alarm.email.enable`）
- 企业微信（WeCom）告警通道（`xxl-job.alarm.wechatwork.enable`）

发布为 Maven JAR 到 Maven Central（`io.github.loulangogogo:xxl-job-alarm`），不是独立可运行应用。

## 常用命令

```bash
# 构建（跳过 GPG 签名）
mvn clean package -Dgpg.skip=true

# 安装到本地 Maven 仓库（供其他项目引用调试）
mvn clean install -Dgpg.skip=true

# 完整发布构建（需要 GPG 密钥已配置）
mvn clean install
```

注意：项目没有测试代码，`-DskipTests` 不需要但也不会有影响。没有配置 lint 工具。

## 架构

### 核心机制

本库通过 Spring Boot `AutoConfiguration.imports` 自动注册，钩入 XXL-Job Admin 的告警体系：

```
XXL-Job Admin（provided 依赖）
  └─ JobAlarm 接口 + 默认 EmailJobAlarm 实现

xxl-job-alarm（本库）
  ├─ RemoveBeanProcessor          — BeanDefinitionRegistryPostProcessor，
  │                                 启动时移除默认 EmailJobAlarm bean
  ├─ NewEmailJobAlarm             — 继承 EmailJobAlarm + @Primary，
  │                                 增加 email.enable 开关
  ├─ WechatWorkJobAlarm           — 实现 JobAlarm，企业微信告警
  ├─ WxCpProperties               — @ConfigurationProperties("wechat.work")
  └─ XxlJobConfiguration          — @Configuration + @ComponentScan
```

`RemoveBeanProcessor` 先移除原始 `EmailJobAlarm`，再由 `NewEmailJobAlarm`（继承原类并加开关）替代。两个告警实现（`NewEmailJobAlarm`、`WechatWorkJobAlarm`）都实现 `JobAlarm` 接口，XXL-Job 的告警分发器会依次调用两者，各自检查自己的 enable 开关。

### 关键依赖

- `com.xuxueli:xxl-job-admin:3.4.2` — scope `provided`，由宿主应用提供
- `com.binarywang:weixin-java-cp:4.8.4.B` — 企业微信 SDK
- Java 17

### 配置项

| 配置键 | 说明 |
|---|---|
| `xxl-job.alarm.email.enable` | 是否启用邮件告警 |
| `xxl-job.alarm.wechatwork.enable` | 是否启用企业微信告警 |
| `wechat.work.corp-id` | 企业微信企业 ID |
| `wechat.work.corp-secret` | 企业微信应用密钥 |
| `wechat.work.agent-id` | 企业微信应用 AgentId |

## 注意事项

- 所有注释、文档、提交信息使用中文
- 企业微信告警的接收人来自任务的 "alarmEmail" 字段（逗号分隔的用户 ID）
- `WechatWorkJobAlarm.initWxCpService()` 每次 `doAlarm()` 调用都会重新创建 `WxCpServiceImpl` 实例
- README 中的依赖版本示例可能未与 pom.xml 同步，修改版本号时需同步更新 README
