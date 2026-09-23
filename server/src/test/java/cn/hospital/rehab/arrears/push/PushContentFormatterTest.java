package cn.hospital.rehab.arrears.push;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushContentFormatterTest {
    @Test
    void replacesAnOldSystemLinkAndKeepsTheStandardFooterAtTheEnd() {
        String content = "原推送内容\n\n详情请点击康复医院运营管理系统查看（院内内网访问）：\nhttp://oa.kfyy.local/arrears";

        assertThat(PushContentFormatter.withSystemLink(content)).isEqualTo(
                "原推送内容\n\n详情请点击康复医院运营管理系统查看（院内内网访问）：\nhttp://172.16.196.112");
    }
}