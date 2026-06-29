package io.github.loulangogogo.xxl.job.admin.alarm.impl;

import com.xxl.job.admin.business.model.XxlJobGroup;
import com.xxl.job.admin.business.model.XxlJobInfo;
import com.xxl.job.admin.business.model.XxlJobLog;
import com.xxl.job.admin.business.scheduler.alarm.JobAlarm;
import com.xxl.job.admin.business.scheduler.config.XxlJobAdminBootstrap;
import com.xxl.job.admin.framework.util.I18nUtil;
import com.xxl.job.core.context.XxlJobContext;
import io.github.loulangogogo.water.json.JsonTool;
import io.github.loulangogogo.water.tool.StrTool;
import io.github.loulangogogo.wood.http.tool.HttpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/*********************************************************
 ** 企业微信群机器人Webhook告警实现
 ** <p>
 ** 复用任务的【告警邮箱】字段存储Webhook Key（逗号分隔支持多Key），
 ** 避免修改XXL-Job数据库表结构即可扩展告警通道。
 ** 消息采用Markdown格式，包含执行器、任务ID、描述、告警类型和详情。
 ** </p>
 **
 ** @author loulan
 ** @since 17
 *********************************************************/
@Component
public class WechatWorkJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(WechatWorkJobAlarm.class);

    @Value("${xxl-job.alarm.wechatwork.enable:false}")
    private boolean enable = false;

    /** 企业微信Webhook推送地址，{}由机器人Key替换 */
    private static final String url = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key={}";

    /**
     * 执行企业微信Webhook告警
     *
     * @param info   任务信息，alarmEmail字段存储Webhook Key列表（逗号分隔）
     * @param jobLog 任务执行日志，包含触发/执行结果和日志详情
     * @return true-所有接收人发送成功，false-任一接收人发送失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {

        if (!enable) {
            logger.warn(">>>>>>>>>>> xxl-job, 企业微信告警通知功能没有开启, JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        boolean alarmResult = true;

        if (info == null || info.getAlarmEmail() == null || info.getAlarmEmail().trim().isEmpty()) {
            logger.warn(">>>>>>>>>>> xxl-job, 任务未配置告警接收人，JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        String content = buildMessageContent(info, jobLog);

        Set<String> receiverSet = new HashSet<>(Arrays.asList(info.getAlarmEmail().split(",")));

        for (String receiver : receiverSet) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("msgtype", "markdown");
                Map<String, Object> markdown = new HashMap<>();
                markdown.put("content", content);
                body.put("markdown", markdown);

                String res = HttpTool.POST.toStr(StrTool.format(url, receiver), JsonTool.toJson(body));
                logger.info(">>>>>>>>>>> xxl-job, 企业微信告警消息发送完成，返回结果:{}",res);
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> xxl-job, 企业微信告警消息发送失败，接收人:{}, JobLogId:{}",
                        receiver, jobLog.getId(), e);
                // 单个接收人失败不中断其他接收人，但标记整体结果为失败
                alarmResult = false;
            }
        }

        return alarmResult;
    }


    /**
     * 构建告警详情：JobLogId + 非成功阶段的触发/执行信息
     *
     * @param info   任务信息（当前未使用，保留以统一接口签名）
     * @param jobLog 任务执行日志，包含触发码、执行码等
     * @return 格式化的告警详情字符串
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
     * 构建企业微信Markdown告警消息体
     *
     * @param info   任务信息，包含任务组ID、任务ID、任务描述等
     * @param jobLog 任务执行日志
     * @return 格式化的Markdown消息字符串
     */
    private String buildMessageContent(XxlJobInfo info, XxlJobLog jobLog) {
        XxlJobGroup group = XxlJobAdminBootstrap.getInstance().getXxlJobGroupMapper().load(Integer.valueOf(info.getJobGroup()));
        String groupName = group != null ? group.getTitle() : "未知任务组";
        String alarmContent = buildAlarmContent(info, jobLog);

        StringBuilder content = new StringBuilder();
        content.append("# 【分布式任务调度平台｜XXL-JOB】\n");
        content.append("> 执行器：").append(groupName).append("\n");
        content.append("> 任务ID：").append(info.getId()).append("\n");
        content.append("> 任务描述：").append(info.getJobDesc()).append("\n");
        content.append("> 告警类型：").append(I18nUtil.getString("jobconf_monitor_alarm_type")).append("\n");
        // 告警内容使用分隔线+引用格式，与头部信息形成视觉层次
        content.append("> 告警内容：\n")
                .append("> ------------------\n")
                .append("> ").append(alarmContent);
        return content.toString();
    }
}
