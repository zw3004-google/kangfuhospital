package cn.hospital.rehab.arrears.push;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="wecom") public record WeComProperties(String corpId,Integer agentId,String secret,String apiBase){public String base(){return apiBase==null||apiBase.isBlank()?"https://qyapi.weixin.qq.com":apiBase;}}
