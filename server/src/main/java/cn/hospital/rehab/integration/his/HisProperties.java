package cn.hospital.rehab.integration.his;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "his")
public class HisProperties {
    private String gatewayUrl = "";
    private String organizationCode = "";
    private String applicationId = "";
    private String licenseId = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private int pageSize = 200;
    private int maxPages = 1000;

    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String value) { gatewayUrl = value == null ? "" : value.trim(); }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String value) { organizationCode = value == null ? "" : value.trim(); }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String value) { applicationId = value == null ? "" : value.trim(); }
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String value) { licenseId = value == null ? "" : value.trim(); }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration value) { connectTimeout = value; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration value) { readTimeout = value; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int value) { pageSize = value; }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int value) { maxPages = value; }

    public void validate() {
        if (gatewayUrl.isBlank()) throw new IllegalStateException("HIS网关地址未配置");
        if (organizationCode.isBlank()) throw new IllegalStateException("HIS机构号未配置");
        if (applicationId.isBlank()) throw new IllegalStateException("HIS应用标识未配置");
        if (licenseId.isBlank()) throw new IllegalStateException("HIS授权标识未配置");
        if (pageSize < 1 || pageSize > 1000) throw new IllegalStateException("HIS分页大小必须在1到1000之间");
        if (maxPages < 1) throw new IllegalStateException("HIS最大分页数必须大于0");
    }
}
