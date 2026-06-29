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
 ** 钉钉群机器人Webhook告警实现
 ** <p>
 ** 复用任务的【告警邮箱】字段存储凭证，支持两种安全模式：
 ** - 纯access_token：适用于IP白名单/关键词模式
 ** - access_token&secret：适用于加签模式（推荐），自动进行HmacSHA256签名
 ** </p>
 **
 ** @author loulan
 ** @since 17
 *********************************************************/
@Component
public class DingtalkJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(DingtalkJobAlarm.class);

    @Value("${xxl-job.alarm.dingtalk.enable:false}")
    private boolean enable = false;

    /** 钉钉Webhook推送地址，{}由access_token替换 */
    private static final String url = "https://oapi.dingtalk.com/robot/send?access_token={}";



    /**
     * 执行钉钉Webhook告警
     * <p>
     * 根据接收人凭证格式自动选择签名策略：
     * 纯token直接发送；token&secret则先计算HmacSHA256签名再附加到URL参数。
     *
     * @param info   任务信息，alarmEmail字段存储凭证列表（逗号分隔）
     * @param jobLog 任务执行日志
     * @return true-所有接收人发送成功，false-任一接收人发送失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {

        if (!enable) {
            logger.warn(">>>>>>>>>>> xxl-job, 钉钉告警通知功能没有开启, JobId:{}", info != null ? info.getId() : "null");
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
                markdown.put("title", "分布式任务调度平台｜XXL-JOB");
                markdown.put("text",content);
                body.put("markdown", markdown);

                // 根据凭证格式构建URL：纯token直接替换，token&secret需追加签名参数
                String receiverUrl = url;
                String[] receiverAndSecret = info.getAlarmEmail().split("&");
                if (receiverAndSecret.length == 1) {
                    // IP白名单/关键词模式：只需access_token
                    String accessToken = receiverAndSecret[0];
                    receiverUrl = StrTool.format(receiverUrl, accessToken);

                } else if (receiverAndSecret.length == 2) {
                    // 加签模式：需拼接timestamp和HmacSHA256签名
                    receiverUrl += "&timestamp={}&sign={}";
                    String secret = receiverAndSecret[1];
                    String accessToken = receiverAndSecret[0];

                    Long timestamp = System.currentTimeMillis();
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
                // 单个接收人失败不中断其他接收人，但标记整体结果为失败
                alarmResult = false;
            }
        }

        return alarmResult;
    }

    /**
     * 生成钉钉Webhook加签签名
     * <p>
     * 签名算法：HmacSHA256(timestamp + "\n" + secret) → Base64 → URLEncode。
     * timestamp同时作为URL参数传递，钉钉服务端据此校验请求时效性（有效期1小时）。
     *
     * @param timestamp 当前毫秒级时间戳
     * @param secret    机器人加签密钥
     * @return URL编码后的签名字符串
     * @throws Exception 当HMAC计算或编码异常时抛出
     * @author loulan
     */
    public static String sign(Long  timestamp,String secret) throws Exception{
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        // Base64 → URL编码，这是钉钉接口要求的签名格式
        String sign = URLEncoder.encode(new String(Base64Tool.toEncode(signData)),"UTF-8");
        return sign;
    }

    /**
     * 构建告警详情：JobLogId + 非成功阶段的触发/执行信息
     *
     * @param info   任务信息（当前未使用，保留以统一接口签名）
     * @param jobLog 任务执行日志
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
     * 构建钉钉Markdown告警消息体
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
        content.append("- 执行器：").append(groupName).append("\n");
        content.append("- 任务ID：").append(info.getId()).append("\n");
        content.append("- 任务描述：").append(info.getJobDesc()).append("\n");
        content.append("- 告警类型：").append(I18nUtil.getString("jobconf_monitor_alarm_type")).append("\n");
        // 告警内容使用引用格式，与列表项形成视觉区分
        content.append("- 告警内容：\n")
                .append("> ").append(alarmContent);
        return content.toString();
    }
}
