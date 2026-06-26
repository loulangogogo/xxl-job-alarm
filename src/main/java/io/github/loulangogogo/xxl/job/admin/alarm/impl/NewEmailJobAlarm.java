package io.github.loulangogogo.xxl.job.admin.alarm.impl;

import com.xxl.job.admin.business.model.XxlJobInfo;
import com.xxl.job.admin.business.model.XxlJobLog;
import com.xxl.job.admin.business.scheduler.alarm.impl.EmailJobAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


/**
 * 自定义邮件任务告警实现类
 * <p>
 * 继承自 XXL-Job 原生的邮件告警实现，使用 @Primary 注解优先注入
 * </p>
 *
 * @author loulan
 */
@Component
@Primary
public class NewEmailJobAlarm extends com.xxl.job.admin.business.scheduler.alarm.impl.EmailJobAlarm {
    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);

    @Value("${xxl-job.alarm.email.enable:false}")
    private boolean enable = false;

    /**
     * 执行任务告警
     * <p>
     * 当任务执行失败或达到告警条件时触发，调用父类的告警实现发送邮件通知
     * </p>
     *
     * @param info    任务信息，包含任务配置和告警接收人等
     * @param jobLog  任务日志，包含任务执行结果和日志信息
     * @return true-告警成功，false-告警失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog){

        // 如果没有开启邮箱通知，那么不进行通知
        if (!enable) {
            logger.warn(">>>>>>>>>>> xxl-job, email job alarm not enable, JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        return super.doAlarm(info, jobLog);
    }
}
