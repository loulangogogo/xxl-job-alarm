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
 * 带开关控制的邮件告警实现
 *
 * <p>原生EmailJobAlarm无法通过配置关闭邮件告警，一旦配置了告警邮箱就必定发送。
 * 本类通过继承+@Primary替换原生实现，增加{@code xxl-job.alarm.email.enable}开关，
 * 使邮件告警可以独立控制启停，默认关闭。
 *
 * @author loulan
 * @see com.xxl.job.admin.business.scheduler.alarm.impl.EmailJobAlarm
 */
@Component
@Primary
public class NewEmailJobAlarm extends com.xxl.job.admin.business.scheduler.alarm.impl.EmailJobAlarm {
    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);

    @Value("${xxl-job.alarm.email.enable:false}")
    private boolean enable = false;

    /**
     * 执行邮件告警，受开关控制
     *
     * @param info   任务信息，包含任务配置、告警接收人列表等
     * @param jobLog 任务执行日志，包含触发/执行结果和日志详情
     * @return true-告警处理成功（含开关关闭跳过的情况），false-告警发送失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog){

        // 开关关闭时返回true而非false，避免上层误判为告警失败而触发重试或异常处理
        if (!enable) {
            logger.warn(">>>>>>>>>>> xxl-job, email job alarm not enable, JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        return super.doAlarm(info, jobLog);
    }
}
