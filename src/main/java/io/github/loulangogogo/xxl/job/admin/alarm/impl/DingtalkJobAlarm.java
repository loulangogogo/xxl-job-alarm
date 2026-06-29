package io.github.loulangogogo.xxl.job.admin.alarm.impl;

import com.xxl.job.admin.business.model.XxlJobGroup;
import com.xxl.job.admin.business.model.XxlJobInfo;
import com.xxl.job.admin.business.model.XxlJobLog;
import com.xxl.job.admin.business.scheduler.alarm.JobAlarm;
import com.xxl.job.admin.business.scheduler.config.XxlJobAdminBootstrap;
import com.xxl.job.admin.framework.util.I18nUtil;
import com.xxl.job.core.context.XxlJobContext;
import io.github.loulangogogo.water.crypto.Base64Tool;
import io.github.loulangogogo.water.json.JsonTool;
import io.github.loulangogogo.water.tool.StrTool;
import io.github.loulangogogo.wood.http.tool.HttpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.*;

/*********************************************************
 ** 企业微信通知服务类
 **
 ** @author loulan
 ** @since 17
 *********************************************************/
@Component
public class DingtalkJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(DingtalkJobAlarm.class);

    @Value("${xxl-job.alarm.dingtalk.enable:false}")
    private boolean enable = false;

    // 消息推送地址
    private static final String url = "https://oapi.dingtalk.com/robot/send?access_token={}";



    /**
     * 执行任务告警
     * <p>
     * 当任务执行失败或达到告警条件时触发，通过钉钉发送告警通知
     * </p>
     *
     * @param info    任务信息，包含任务配置和告警接收人等
     * @param jobLog  任务日志，包含任务执行结果和日志信息
     * @return true-告警成功，false-告警失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {

        // 如果没有开启钉钉通知，那么不进行通知
        if (!enable) {
            logger.warn(">>>>>>>>>>> xxl-job, 钉钉告警通知功能没有开启, JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        boolean alarmResult = true;

        // 校验告警接收人
        if (info == null || info.getAlarmEmail() == null || info.getAlarmEmail().trim().isEmpty()) {
            logger.warn(">>>>>>>>>>> xxl-job, 任务未配置告警接收人，JobId:{}", info != null ? info.getId() : "null");
            return true;
        }

        // 构建消息内容
        String content = buildMessageContent(info, jobLog);

        // 发送消息给所有接收人
        Set<String> receiverSet = new HashSet<>(Arrays.asList(info.getAlarmEmail().split(",")));

        for (String receiver : receiverSet) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("msgtype", "markdown");
                Map<String, Object> markdown = new HashMap<>();
                markdown.put("title", "分布式任务调度平台｜XXL-JOB");
                markdown.put("text",content);
                body.put("markdown", markdown);

                // 如果用户选择的是秘钥的方式，那么需要进行秘钥验签
                String receiverUrl = url;
                String[] receiverAndSecret = info.getAlarmEmail().split("&");
                if (receiverAndSecret.length == 1) {
                    String accessToken = receiverAndSecret[0];
                    receiverUrl = StrTool.format(receiverUrl, accessToken);

                } else if (receiverAndSecret.length == 2) {
                    // 这个的输入是accessToken&secret
                    receiverUrl += "&timestamp={}&sign={}";
                    // 获取密钥
                    String secret = receiverAndSecret[1];
                    String accessToken = receiverAndSecret[0];

                    // 获取时间戳
                    Long timestamp = System.currentTimeMillis();
                    // 获取签名
                    String sign = sign(timestamp, secret);

                    receiverUrl = StrTool.format(receiverUrl,accessToken, timestamp, sign);
                } else {

                    logger.error(">>>>>>>>>>> xxl-job, 钉钉告警消接收人信息错误，接收人:{}, JobLogId:{}",
                            receiver, jobLog.getId());
                    continue;
                }

                String res = HttpTool.POST.toStr(receiverUrl, JsonTool.toJson(body));
                logger.info(">>>>>>>>>>> xxl-job, 钉钉告警消息发送完成，返回结果:{}",res);
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> xxl-job, 钉钉告警消息发送失败，接收人:{}, JobLogId:{}",
                        receiver, jobLog.getId(), e);
                alarmResult = false;
            }
        }

        return alarmResult;
    }

    /**
     * 生成钉钉机器人Webhook请求的签名
     * <p>
     * 使用HmacSHA256算法对时间戳和密钥进行签名，并将结果进行Base64编码和URL编码，
     * 用于钉钉机器人消息发送时的身份验证。
     *
     * @param timestamp 当前时间戳（毫秒级）
     * @param secret    钉钉机器人的加签密钥
     * @return URL编码后的签名字符串
     * @exception Exception 当加密或编码过程发生异常时抛出
     * @author :loulan
     * */
    public static String sign(Long  timestamp,String secret) throws Exception{
        // 拼接待签名字符串：时间戳 + 换行符 + 密钥
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        // 对签名数据进行Base64编码后再进行URL编码
        String sign = URLEncoder.encode(new String(Base64Tool.toEncode(signData)),"UTF-8");
        return sign;
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
        content.append("- 执行器：").append(groupName).append("\n");
        content.append("- 任务ID：").append(info.getId()).append("\n");
        content.append("- 任务描述：").append(info.getJobDesc()).append("\n");
        content.append("- 告警类型：").append(I18nUtil.getString("jobconf_monitor_alarm_type")).append("\n");
        content.append("- 告警内容：\n")
                .append("> ------------------")
                .append("> ").append(alarmContent);
        return content.toString();
    }
}
