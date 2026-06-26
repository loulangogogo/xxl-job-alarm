package io.github.loulangogogo.xxl.job.admin.alarm.impl;

import com.xxl.job.admin.business.model.XxlJobGroup;
import com.xxl.job.admin.business.model.XxlJobInfo;
import com.xxl.job.admin.business.model.XxlJobLog;
import com.xxl.job.admin.business.scheduler.alarm.JobAlarm;
import com.xxl.job.admin.business.scheduler.config.XxlJobAdminBootstrap;
import com.xxl.job.core.context.XxlJobContext;
import io.github.loulangogogo.xxl.job.admin.alarm.model.WxCpProperties;
import jakarta.annotation.Resource;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*********************************************************
 ** 企业微信通知服务类
 **
 ** @author loulan
 ** @since 17
 *********************************************************/
@Component
public class WechatWorkJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(WechatWorkJobAlarm.class);

    @Value("${xxl-job.alarm.wechatwork.enable:false}")
    private boolean enable = false;

    @Resource
    private WxCpProperties wxCpProperties;

    /**
     * 执行任务告警
     * <p>
     * 当任务执行失败或达到告警条件时触发，通过企业微信发送告警通知
     * </p>
     *
     * @param info    任务信息，包含任务配置和告警接收人等
     * @param jobLog  任务日志，包含任务执行结果和日志信息
     * @return true-告警成功，false-告警失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {

        // 如果没有开启企业微信通知，那么不进行通知
        if (!enable) {
            logger.warn(">>>>>>>>>>> xxl-job, wechatwork job alarm not enable, JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        boolean alarmResult = true;

        // 校验配置信息
        if (wxCpProperties == null || wxCpProperties.getCorpId() == null
                || wxCpProperties.getCorpSecret() == null || wxCpProperties.getAgentId() == null) {
            logger.error(">>>>>>>>>>> xxl-job, 企业微信告警配置不完整，请检查 wx.cp 配置");
            return false;
        }

        // 校验告警接收人
        if (info == null || info.getAlarmEmail() == null || info.getAlarmEmail().trim().isEmpty()) {
            logger.warn(">>>>>>>>>>> xxl-job, 任务未配置告警接收人，JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        // 初始化企业微信服务
        WxCpServiceImpl wxCpService = initWxCpService();
        if (wxCpService == null) {
            logger.error(">>>>>>>>>>> xxl-job, 企业微信服务初始化失败");
            return false;
        }

        // 构建消息内容
        String content = buildMessageContent(info, jobLog);

        // 发送消息给所有接收人
        Set<String> receiverSet = new HashSet<>(Arrays.asList(info.getAlarmEmail().split(",")));

        for (String receiver : receiverSet) {
            try {
                WxCpMessage message = WxCpMessage.MARKDOWN()
                        .toUser(receiver)
                        .content(content)
                        .build();

                wxCpService.getMessageService().send(message);
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> xxl-job, 企业微信告警消息发送失败，接收人:{}, JobLogId:{}",
                        receiver, jobLog.getId(), e);
                alarmResult = false;
            }
        }

        return alarmResult;
    }

    /**
     * 初始化企业微信服务
     *
     * @return 企业微信服务实例
     */
    private WxCpServiceImpl initWxCpService() {
        try {
            WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
            config.setCorpId(wxCpProperties.getCorpId());
            config.setCorpSecret(wxCpProperties.getCorpSecret());
            config.setAgentId(wxCpProperties.getAgentId());

            WxCpServiceImpl wxCpService = new WxCpServiceImpl();
            wxCpService.setWxCpConfigStorage(config);
            return wxCpService;
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> xxl-job, 企业微信服务初始化异常", e);
            return null;
        }
    }

    /**
     * 构建告警内容
     *
     * @param info    任务信息
     * @param jobLog  任务日志
     * @return 告警内容字符串
     */
    private String buildAlarmContent(XxlJobInfo info, XxlJobLog jobLog) {
        StringBuilder alarmContent = new StringBuilder();
        alarmContent.append("告警任务 JobLogId=").append(jobLog.getId());

        if (jobLog.getTriggerCode() != XxlJobContext.HANDLE_CODE_SUCCESS) {
            alarmContent.append("\n触发信息：").append(jobLog.getTriggerMsg());
        }

        if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != XxlJobContext.HANDLE_CODE_SUCCESS) {
            alarmContent.append("\n执行结果：").append(jobLog.getHandleMsg());
        }

        return alarmContent.toString();
    }

    /**
     * 构建消息内容
     *
     * @param info          任务信息
     * @param jobLog  任务日志
     * @return 格式化的消息内容
     */
    private String buildMessageContent(XxlJobInfo info, XxlJobLog jobLog) {
        // 获取任务组信息
        XxlJobGroup group = XxlJobAdminBootstrap.getInstance().getXxlJobGroupMapper().load(Integer.valueOf(info.getJobGroup()));
        String groupName = group != null ? group.getTitle() : "未知任务组";
        // 构建告警内容
        String alarmContent = buildAlarmContent(info, jobLog);

        // 构建消息内容
        StringBuilder content = new StringBuilder();
        content.append("# 【分布式任务调度平台｜XXL-JOB】\n");
        content.append("> 执行器：").append(groupName).append("\n");
        content.append("> 任务ID：").append(info.getId()).append("\n");
        content.append("> 任务描述：").append(info.getJobDesc()).append("\n");
        content.append("> 告警类型：任务执行失败\n");
        content.append("> 告警内容：\n")
                .append("```")
                .append(alarmContent)
                .append("```");
        return content.toString();
    }
}
