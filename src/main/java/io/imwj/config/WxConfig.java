package io.imwj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置类
 * @author langao_q
 * @since 2020-07-21 15:13
 */
@Data
@Component
@ConfigurationProperties("weixin")
public class WxConfig {

    /**
     * 微信appid
     */
    private String appId;
    /**
     * 微信appSecret
     */
    private String appSecret;
    /**
     * 微信token
     */
    private String token;

    /**
     * 获取信息地址
     */
    private String urlPath;

}
