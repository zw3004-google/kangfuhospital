package cn.hospital.rehab.system.user;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LoginNameGenerator {
    private final HanyuPinyinOutputFormat format;

    public LoginNameGenerator() {
        format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    public String fromDisplayName(String displayName) {
        StringBuilder result = new StringBuilder();
        for (char character : displayName.trim().toCharArray()) {
            if (character <= 127 && Character.isLetterOrDigit(character)) {
                result.append(Character.toLowerCase(character));
                continue;
            }
            try {
                String[] values = PinyinHelper.toHanyuPinyinStringArray(character, format);
                if (values != null && values.length > 0) result.append(values[0]);
            } catch (BadHanyuPinyinOutputFormatCombination exception) {
                throw new IllegalStateException("姓名拼音转换失败", exception);
            }
        }
        String loginName = result.toString().toLowerCase(Locale.ROOT);
        if (loginName.isBlank()) throw new IllegalArgumentException("姓名无法生成登录名，请检查姓名");
        return loginName;
    }
}
