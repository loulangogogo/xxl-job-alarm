package io.github.loulangogogo.xxl.job.admin.alarm.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 微信企业号配置属性类
 * <p>
 * 用于配置微信企业号相关参数，包括企业ID、应用密钥和应用ID
 * </p>
 *
 * @author loulan
 */
@Component
@ConfigurationProperties(prefix = "wechat.work")
public class WxCpProperties {

    /**
     * 微信企业号 corpId
     */
    private String corpId;
    /**
     * 微信企业号 corpSecret
     */
    private String corpSecret;
    /**
     * 微信企业号应用 ID
     */
    private Integer agentId;

    /**
     * 默认空构造方法
     */
    public WxCpProperties() {
    }

    /**
     * 获取微信企业号 corpId
     *
     * @return 微信企业号 corpId
     */
    public String getCorpId() {
        return corpId;
    }

    /**
     * 设置微信企业号 corpId
     *
     * @param corpId 微信企业号 corpId
     */
    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    /**
     * 获取微信企业号 corpSecret
     *
     * @return 微信企业号 corpSecret
     */
    public String getCorpSecret() {
        return corpSecret;
    }

    /**
     * 设置微信企业号 corpSecret
     *
     * @param corpSecret 微信企业号 corpSecret
     */
    public void setCorpSecret(String corpSecret) {
        this.corpSecret = corpSecret;
    }

    /**
     * 获取微信企业号应用 ID
     *
     * @return 微信企业号应用 ID
     */
    public Integer getAgentId() {
        return agentId;
    }

    /**
     * 设置微信企业号应用 ID
     *
     * @param agentId 微信企业号应用 ID
     */
    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }
}
