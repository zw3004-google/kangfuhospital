package cn.hospital.rehab.arrears.push;

public final class PushContentFormatter {
    public static final String SYSTEM_LINK = "http://172.16.196.112";
    public static final String FOOTER_LABEL = "详情请点击康复医院运营管理系统查看（院内内网访问）：";
    public static final String FOOTER = FOOTER_LABEL + "\n" + SYSTEM_LINK;
    private PushContentFormatter() {}
    public static String withSystemLink(String content) {
        String body = content == null ? "" : content.trim();
        int existingFooter = body.lastIndexOf(FOOTER_LABEL);
        if (existingFooter >= 0) body = body.substring(0, existingFooter).trim();
        return body.isBlank() ? FOOTER : body + "\n\n" + FOOTER;
    }
}