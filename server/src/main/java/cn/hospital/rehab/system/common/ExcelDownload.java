package cn.hospital.rehab.system.common;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ExcelDownload {
    private ExcelDownload() { }

    public static void write(HttpServletResponse response, String filename, byte[] content) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }
}
