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
  4. 按脚本提示编辑 /etc/kangfu/kangfu.env，然后执行：
       systemctl restart kangfu-server
  5. 执行：bash scripts/verify.sh

完整步骤、参数和排障方法见随包部署手册。
