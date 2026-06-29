# XXL-Job-Alarm

XXL-Job 告警扩展模块，支持**邮件**、**企业微信**、**钉钉**三种告警通知方式，且每种方式均支持独立的开关控制。

## 项目简介

本项目基于 [XXL-Job](https://github.com/xuxueli/xxl-job) 分布式任务调度平台，针对其告警功能进行扩展。XXL-Job 原生仅支持邮件告警，本项目在此基础上：

- 增加了**企业微信机器人 Webhook** 告警支持
- 增加了**钉钉机器人 Webhook** 告警支持
- 为邮件、企业微信、钉钉三种告警方式均增加了**开关控制**

## 项目结构

```
xxl-job-alarm/
└── src/main/java/io/github/loulangogogo/xxl/job/admin/alarm/
    ├── config/
    │   ├── RemoveBeanProcessor.java      # Bean 移除处理器，移除 XXL-Job 默认的 EmailJobAlarm
    │   └── XxlJobConfiguration.java      # Spring 配置类，启用组件扫描
    └── impl/
        ├── NewEmailJobAlarm.java         # 邮件告警实现（支持开关控制）
        ├── WechatWorkJobAlarm.java       # 企业微信 Webhook 告警实现
        └── DingtalkJobAlarm.java         # 钉钉 Webhook 告警实现
```

### 核心类说明

| 类名 | 说明 |
|------|------|
| `RemoveBeanProcessor` | Spring `BeanDefinitionRegistryPostProcessor`，在容器启动时移除 XXL-Job 默认的 `EmailJobAlarm`，使自定义实现生效 |
| `XxlJobConfiguration` | Spring 配置类，通过 `@ComponentScan` 扫描告警模块所在包，并通过 `AutoConfiguration.imports` 实现 Spring Boot 自动装配 |
| `NewEmailJobAlarm` | 继承 XXL-Job 原生 `EmailJobAlarm`，使用 `@Primary` 注解优先注入，增加开关控制 |
| `WechatWorkJobAlarm` | 企业微信告警实现，通过企业微信群机器人 Webhook 发送 Markdown 消息 |
| `DingtalkJobAlarm` | 钉钉告警实现，通过钉钉群机器人 Webhook 发送 Markdown 消息，支持密钥加签验证 |

## 版本对应

本模块版本号与所适配的 XXL-Job 版本保持对应关系，当前适配 **XXL-Job 3.4.2**。

| 模块版本 | XXL-Job 版本 |
|----------|-------------|
| 3.4.2.x  | 3.4.2       |

## 快速开始

### 引入依赖

在 XXL-Job Admin 项目的 `pom.xml` 中引入本模块：

```xml
<dependency>
    <groupId>io.github.loulangogogo</groupId>
    <artifactId>xxl-job-alarm</artifactId>
    <version>3.4.2.1</version>
</dependency>
```

### 告警开关配置

在 `application.properties` 中按需开启各告警通道（默认均关闭）：

```properties
# 是否启用邮件告警（默认：false）
xxl-job.alarm.email.enable=true

# 是否启用企业微信告警（默认：false）
xxl-job.alarm.wechatwork.enable=true

# 是否启用钉钉告警（默认：false）
xxl-job.alarm.dingtalk.enable=true
```

## 邮件通知配置

邮件通知复用 XXL-Job 原生的 Spring Boot 邮件能力，需在 `application.properties` 中配置 SMTP 参数：

```properties
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

| 配置项 | 说明 |
|--------|------|
| `spring.mail.host` | SMTP 服务器地址，如 `smtp.qq.com`、`smtp.exmail.qq.com` |
| `spring.mail.port` | SMTP 端口，通常为 `25` 或 `465`（SSL） |
| `spring.mail.username` | 发件人账号 |
| `spring.mail.password` | 发件人密码或授权码 |
| `spring.mail.from` | 发件人邮箱地址 |

**任务接收人配置**：在 XXL-Job 管理后台，为任务填写"告警邮箱"字段，多个接收人以逗号分隔。

## 企业微信通知配置

企业微信通知通过**群机器人 Webhook** 发送消息，无需创建企业应用。

### 创建群机器人

1. 在企业微信群聊中点击右上角 `...` → 添加群机器人 → 新建机器人
2. 设置机器人名称和头像
3. 创建完成后复制 Webhook 地址中的 `key` 参数

### 配置任务接收人

在 XXL-Job 管理后台，将任务的"告警邮箱"字段填写为机器人的 **Webhook Key**，多个机器人以逗号分隔：

```
机器人1的key,机器人2的key
```

> **注意**：此处复用了"告警邮箱"字段来存储 Webhook Key，并非真正的邮箱地址。

## 钉钉通知配置

钉钉通知通过**群机器人 Webhook** 发送消息，支持两种安全验证方式。

### 创建群机器人

1. 在钉钉群中点击右上角 `...` → 智能群助手 → 添加机器人 → 自定义（通过 Webhook 接入）
2. 设置机器人名称
3. 选择安全设置（推荐"加签"方式）
4. 复制 Webhook 地址中的 `access_token` 参数

### 配置任务接收人

在 XXL-Job 管理后台，将任务的"告警邮箱"字段填写为 Webhook 凭证。支持两种格式：

**仅 access_token（适用于 IP 白名单或自定义关键词方式）：**

```
access_token值
```

**access_token + secret（适用于加签方式，推荐）：**

```
access_token值&secret值
```

多个机器人以逗号分隔：

```
token1&secret1,token2&secret2
```

> **注意**：此处复用了"告警邮箱"字段来存储 Webhook 凭证，并非真正的邮箱地址。

## 许可证

本项目遵循 [Apache License 2.0](LICENSE)。
