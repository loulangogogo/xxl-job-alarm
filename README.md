# XXL-Job-Alarm

XXL-Job 告警扩展模块，提供邮件通知和企业微信通知功能。

## 项目来源

本项目基于 [XXL-Job](https://github.com/xuxueli/xxl-job) 分布式任务调度平台，针对其告警功能进行扩展。XXL-Job 原生仅支持邮件告警，本项目在此基础上增加了企业微信告警支持，并为两种告警方式都添加了开关控制。

## 项目结构

```
xxl-job-alarm/
├── src/main/java/io/github/loulangogogo/xxl/job/admin/alarm/
│   ├── config/
│   │   ├── RemoveBeanProcessor.java      # Bean 移除处理器，排除默认告警实现
│   │   └── XxlJobConfiguration.java      # Spring 配置类
│   ├── impl/
│   │   ├── NewEmailJobAlarm.java         # 邮件告警实现（支持开关控制）
│   │   └── WechatWorkJobAlarm.java       # 企业微信告警实现
│   └── model/
│       └── WxCpProperties.java           # 企业微信配置属性类
└── pom.xml
```

### 核心类说明

| 类名 | 说明 |
|------|------|
| `RemoveBeanProcessor` | Spring Bean 后置处理器，用于移除 XXL-Job 默认的 `EmailJobAlarm`，使自定义告警实现生效 |
| `XxlJobConfiguration` | Spring 配置类，启用组件扫描 |
| `NewEmailJobAlarm` | 继承自 XXL-Job 原生邮件告警，增加了开关控制 |
| `WechatWorkJobAlarm` | 企业微信告警实现，使用 `weixin-java-cp` SDK 发送消息 |
| `WxCpProperties` | 企业微信配置属性，映射 `wechat.work` 前缀的配置项 |

## 快速开始

### 引入依赖

在 XXL-Job Admin 项目中引入本模块：

```xml
<dependency>
    <groupId>io.github.loulangogogo</groupId>
    <artifactId>xxl-job-alarm</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 配置说明

在 `application.properties` 或 `application.yml` 中添加以下配置：

#### 告警开关配置

```properties
# 是否启用邮件告警（默认：false）
xxl-job.alarm.email.enable=true

# 是否启用企业微信告警（默认：false）
xxl-job.alarm.wechatwork.enable=true
```

## 邮件通知配置

邮件通知使用 Spring Boot 标准的邮件配置，需要在 XXL-Job Admin 的 `application.properties` 中配置 SMTP 相关参数：

```properties
# Spring Boot 邮件配置
spring.mail.host=smtp.qq.com
spring.mail.port=25
spring.mail.username=xxx@qq.com
spring.mail.password=xxx
spring.mail.from=xxx@qq.com
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.socketFactory.class=javax.net.ssl.SSLSocketFactory
```

**配置项说明**：

| 配置项 | 说明 |
|--------|------|
| `spring.mail.host` | SMTP 服务器地址，如 `smtp.qq.com`、`smtp.exmail.qq.com` |
| `spring.mail.port` | SMTP 端口，通常为 25 或 465（SSL） |
| `spring.mail.username` | 发件人账号 |
| `spring.mail.password` | 发件人密码或授权码 |
| `spring.mail.from` | 发件人邮箱地址 |

**任务接收人配置**：在 XXL-Job 管理后台，为任务配置"告警邮箱"字段，多个接收人使用逗号分隔。

## 企业微信通知配置

企业微信通知需要配置企业应用信息：

```properties
# 企业微信配置
wechat.work.corp-id=your-corp-id
wechat.work.corp-secret=your-corp-secret
wechat.work.agent-id=your-agent-id
```

**配置项说明**：

| 配置项 | 说明 |
|--------|------|
| `wechat.work.corp-id` | 企业微信 CorpID，在企业微信管理后台"我的企业"页面获取 |
| `wechat.work.corp-secret` | 企业微信应用的 Secret，在应用管理页面获取 |
| `wechat.work.agent-id` | 企业微信应用的 AgentId，在应用管理页面获取 |

**任务接收人配置**：在 XXL-Job 管理后台，为任务的"告警邮箱"字段配置企业微信用户的 UserId（注意：不是手机号或邮箱），多个接收人使用逗号分隔。

### 企业微信应用创建步骤

1. 登录 [企业微信管理后台](https://work.weixin.qq.com/)
2. 进入"应用管理" -> "自建" -> "创建应用"
3. 填写应用信息，设置可见范围
4. 记录 `AgentId` 和 `Secret`
5. 在"我的企业"页面获取 `CorpID`

## 许可证

本项目遵循 XXL-Job 的 [GPLv2](https://github.com/xuxueli/xxl-job/blob/master/LICENSE) 许可证。
