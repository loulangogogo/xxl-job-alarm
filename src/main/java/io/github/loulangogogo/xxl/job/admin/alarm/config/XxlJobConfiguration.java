package io.github.loulangogogo.xxl.job.admin.alarm.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/*********************************************************
 ** XXL-Job告警模块自动装配入口
 ** <p>
 ** 本类本身不包含业务逻辑，仅作为Spring Boot自动装配的入口点。
 ** 通过META-INF/spring/...AutoConfiguration.imports注册后，
 ** 外部项目引入jar依赖即可自动扫描本包下所有组件，无需手动配置。
 ** </p>
 **
 ** @author loulan
 ** @since  17
 *********************************************************/
@Configuration
@ComponentScan(basePackages = "io.github.loulangogogo.xxl.job.admin.alarm")
public class XxlJobConfiguration {
}
