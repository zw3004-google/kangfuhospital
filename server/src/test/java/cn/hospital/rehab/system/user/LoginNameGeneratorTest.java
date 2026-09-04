package cn.hospital.rehab.system.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginNameGeneratorTest {
    private final LoginNameGenerator generator = new LoginNameGenerator();

    @Test
    void convertsChineseNameToPinyin() {
        assertThat(generator.fromDisplayName("张三")).isEqualTo("zhangsan");
    }

    @Test
    void keepsAsciiAndDropsSeparators() {
        assertThat(generator.fromDisplayName("A-01")).isEqualTo("a01");
    }
}
