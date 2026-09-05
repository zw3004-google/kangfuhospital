康复医院运营管理系统测试环境离线安装包

目标系统：openEuler 22.03 SP3 x86_64
目标地址：172.16.196.111

目录说明：
  app/       后端可执行 JAR 与前端静态文件
  config/    应用、Nginx 和 systemd 配置模板
  scripts/   安装、升级、备份、恢复、检查脚本
  runtime/   已包含 Linux x86_64 Temurin JRE 21
  rpms/      已包含 PostgreSQL 17、Nginx 和全部离线依赖
  checksums/ 文件完整性清单

首次部署：
  1. 将整个目录复制到测试服务器。
  2. 以 root 执行：bash scripts/preflight.sh
  3. 检查通过后执行：bash scripts/install.sh
  4. 按脚本提示编辑 /etc/kangfu/kangfu.env，核对 APP_ALLOWED_ORIGINS；启用 HTTPS 时将 SESSION_COOKIE_SECURE 改为 true，然后执行：
       systemctl restart kangfu-server
  5. 执行：bash scripts/verify.sh

完整步骤、参数和排障方法见随包部署手册。

升级与登录来源排障（2026-09-05 补充）：
  - 升级现有服务器时也必须核对 /etc/kangfu/kangfu.env 中的 APP_ALLOWED_ORIGINS，不能只替换应用文件。
  - 填写浏览器实际访问来源（协议、主机及非默认端口），例如 http://172.16.196.111；多个来源用逗号分隔，不填写路径或通配符。
  - HTTPS 在 Nginx 终止、后端使用 HTTP 时，必须显式填写外部 HTTPS 来源，否则登录可能返回 403 ORIGIN_DENIED（请求来源不受信任）。仅配置转发头不足以解决。
  - 修改后重启 kangfu-server，并验证可信来源携带会话 Cookie/CSRF Token 可进入认证，非可信来源仍被拒绝；不能仅检查首页和健康接口。
  - 腾讯云测试环境使用 /etc/kangfu-test/kangfu.env 和 kangfu-test 服务，配置示例及修复记录见项目 docs/TENCENT_CLOUD_TEST_DEPLOYMENT.md。
