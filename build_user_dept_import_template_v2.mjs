import fs from "node:fs/promises";
import { Workbook, SpreadsheetFile } from "@oai/artifact-tool";

const outputDir = "D:/projects/kangfuhospital/outputs/user_dept_import_v2_20260827";
await fs.mkdir(outputDir, { recursive: true });
const wb = Workbook.create();
const guide = wb.worksheets.add("使用说明");
const dept = wb.worksheets.add("科室导入模板");
const user = wb.worksheets.add("用户导入模板");
const dict = wb.worksheets.add("字段与校验规则");

const NAVY = "#1F4E78";
const PALE = "#F4F8FB";
const INPUT = "#FFF8E1";
const SAMPLE = "#FFF2CC";
const RED = "#FCE8E6";
const BORDER = "#D9E2F3";
const FONT = "Microsoft YaHei";

function base(sheet) {
  sheet.showGridLines = false;
  sheet.getRange("A1:Z250").format.font = { name: FONT, size: 10, color: "#222222" };
}
function header(sheet, range) {
  sheet.getRange(range).format = {
    fill: NAVY,
    font: { name: FONT, size: 10, bold: true, color: "#FFFFFF" },
    horizontalAlignment: "center",
    verticalAlignment: "center",
    wrapText: true,
    borders: { preset: "all", style: "thin", color: BORDER },
  };
}
function body(sheet, range) {
  sheet.getRange(range).format = {
    verticalAlignment: "center",
    wrapText: true,
    borders: { preset: "inside", style: "thin", color: "#E6EAF0" },
  };
}
function title(sheet, text, note, endCol) {
  sheet.getRange(`A1:${endCol}1`).merge();
  sheet.getRange("A1").values = [[text]];
  sheet.getRange("A1").format = { fill: NAVY, font: { name: FONT, size: 18, bold: true, color: "#FFFFFF" }, verticalAlignment: "center" };
  sheet.getRange("A1").format.rowHeight = 34;
  sheet.getRange(`A2:${endCol}2`).merge();
  sheet.getRange("A2").values = [[note]];
  sheet.getRange("A2").format = { fill: PALE, font: { name: FONT, size: 10, color: "#44546A" }, wrapText: true, verticalAlignment: "center" };
  sheet.getRange("A2").format.rowHeight = 30;
}
[guide, dept, user, dict].forEach(base);

// 使用说明
title(guide, "康复医院｜科室与用户导入模板（单科室版）", "已确认：一名用户只属于一个科室；角色不通过Excel导入，用户导入后由管理员在系统中配置。", "G");
guide.getRange("B4:G4").merge();
guide.getRange("A4").values = [["项目"]];
guide.getRange("B4").values = [["规则"]];
header(guide, "A4:G4");
const rules = [
  ["导入顺序", "先导入科室，再导入用户。用户中的科室编码必须已存在。"],
  ["用户唯一键", "企微ID全局唯一。去除首尾空格后按原值匹配，不转换大小写。"],
  ["科室唯一键", "科室编码全局唯一；科室改名时按编码更新名称。"],
  ["用户科室关系", "一名用户只能对应一个科室，科室编码直接维护在用户导入表中。"],
  ["角色配置", "导入文件不包含角色。新用户导入后默认无业务角色，由管理员在系统中分配。"],
  ["重复导入", "企微ID已存在时更新姓名、工号、手机号、登录账号、所属科室和状态，但不修改现有角色。"],
  ["人员调科", "重新导入新的科室编码后更新当前所属科室，并记录调科前后值；历史业务记录保留原科室快照。"],
  ["增量原则", "文件中未出现的用户和科室保持原状态，不自动停用或删除。"],
  ["停用原则", "通过状态字段停用，不物理删除；停用用户不能登录，也不进入企微推送接收范围。"],
  ["企微ID变化", "不能通过普通导入替换，应由管理员执行身份迁移并保留全部历史关联。"],
];
for (let i = 0; i < rules.length; i++) {
  const row = 5 + i;
  guide.getRange(`B${row}:G${row}`).merge();
  guide.getRange(`A${row}`).values = [[rules[i][0]]];
  guide.getRange(`B${row}`).values = [[rules[i][1]]];
}
body(guide, "A5:G14");
guide.getRange("A16:G16").merge();
guide.getRange("A16").values = [["导入执行要求"]];
guide.getRange("A16").format = { fill: "#D9EAF7", font: { name: FONT, bold: true, color: NAVY } };
guide.getRange("A17:G17").values = [["阶段", "新增", "更新", "无变化", "失败", "警告", "处理要求"]];
header(guide, "A17:G17");
guide.getRange("A18:G21").values = [
  ["预检", "唯一键不存在", "唯一键存在且字段变化", "数据完全一致", "必填、重复或引用校验失败", "调科、停用等需关注", "展示统计，用户确认后执行"],
  ["正式导入", "创建用户/科室", "按覆盖规则更新", "跳过", "其他合格行可以继续", "正常导入并提示", "生成批次号和失败明细"],
  ["审计", "", "", "", "", "", "保留原文件、导入人、时间、来源IP及字段变更"],
  ["角色", "新用户无角色", "角色保持不变", "保持不变", "", "", "导入完成后由管理员配置"],
];
body(guide, "A18:G21");
guide.getRange("A23:G23").merge();
guide.getRange("A23").values = [["重要：用户导入成功不代表具备业务权限。新用户默认无角色，必须由管理员在系统中完成角色配置后才能访问相应业务模块。"]];
guide.getRange("A23").format = { fill: RED, font: { name: FONT, bold: true, color: "#9C0006" }, wrapText: true, verticalAlignment: "center" };
guide.getRange("A23").format.rowHeight = 38;
guide.getRange("A17:F21").format.columnWidth = 13;
guide.getRange("G17:G21").format.columnWidth = 34;
guide.freezePanes.freezeRows(4);

// 科室导入
title(dept, "科室导入模板", "必填列以“*”标识。正式导入文件必须保留第4行表头；示例行可删除。", "G");
dept.getRange("A4:G4").values = [["科室编码*", "科室名称*", "上级科室编码", "科室类型*", "状态*", "排序号", "备注"]];
header(dept, "A4:G4");
dept.getRange("A5:G7").values = [
  ["KF001", "康复医学中心", "", "临床科室", "启用", 10, "示例：一级科室"],
  ["KF00101", "神经重症康复病房", "KF001", "病区", "启用", 20, "示例：下级病区"],
  ["YYB", "运营部", "", "职能科室", "启用", 30, "示例：职能科室"],
];
body(dept, "A5:G103");
dept.getRange("A5:G103").format.fill = INPUT;
dept.getRange("A5:G7").format.fill = SAMPLE;
dept.getRange("A5:C103").format.numberFormat = "@";
dept.getRange("D5:D103").dataValidation = { rule: { type: "list", values: ["临床科室", "病区", "职能科室", "其他"] } };
dept.getRange("E5:E103").dataValidation = { rule: { type: "list", values: ["启用", "停用"] } };
dept.getRange("F5:F103").dataValidation = { rule: { type: "whole", operator: "between", formula1: 0, formula2: 999999 } };
dept.tables.add("A4:G103", true, "DepartmentImportV2");
dept.freezePanes.freezeRows(4);
dept.getRange("A:A").format.columnWidth = 18;
dept.getRange("B:B").format.columnWidth = 29;
dept.getRange("C:F").format.columnWidth = 18;
dept.getRange("G:G").format.columnWidth = 34;

// 用户导入
title(user, "用户导入模板", "一行代表一名用户；企微ID唯一；每名用户必须且只能填写一个科室编码；角色不在此表配置。", "H");
user.getRange("A4:H4").values = [["企微ID*", "姓名*", "科室编码*", "工号", "手机号", "登录账号", "状态*", "备注"]];
header(user, "A4:H4");
user.getRange("A5:H7").values = [
  ["zhangsan", "张三", "KF00101", "YS0101", "13800000001", "zhangsan", "启用", "示例：导入后由管理员配置角色"],
  ["lisi", "李四", "KF00101", "HS0201", "13800000002", "lisi", "启用", "示例：同科室不同用户"],
  ["wangwu", "王五", "YYB", "YY0031", "13800000003", "wangwu", "启用", "示例：职能科室用户"],
];
body(user, "A5:H203");
user.getRange("A5:H203").format.fill = INPUT;
user.getRange("A5:H7").format.fill = SAMPLE;
user.getRange("A5:F203").format.numberFormat = "@";
user.getRange("G5:G203").dataValidation = { rule: { type: "list", values: ["启用", "停用"] } };
user.tables.add("A4:H203", true, "UserImportV2");
user.freezePanes.freezeRows(4);
user.getRange("A:A").format.columnWidth = 23;
user.getRange("B:B").format.columnWidth = 14;
user.getRange("C:F").format.columnWidth = 19;
user.getRange("G:G").format.columnWidth = 12;
user.getRange("H:H").format.columnWidth = 38;

// 字段与校验
title(dict, "字段与校验规则", "以下规则用于开发导入预检、正式执行和审计功能。", "F");
dict.getRange("A4:F4").values = [["对象", "字段", "是否必填", "唯一/引用规则", "重复导入处理", "说明"]];
header(dict, "A4:F4");
dict.getRange("A5:F18").values = [
  ["科室", "科室编码", "是", "全局唯一", "按编码更新", "一经被引用不建议修改编码"],
  ["科室", "科室名称", "是", "允许改名", "覆盖名称", "保留名称变更日志"],
  ["科室", "上级科室编码", "否", "必须已存在且不能循环", "覆盖", "父级优先导入"],
  ["科室", "科室类型", "是", "限定枚举", "覆盖", "临床科室/病区/职能科室/其他"],
  ["科室", "状态", "是", "启用/停用", "覆盖", "停用前检查是否仍有启用用户"],
  ["用户", "企微ID", "是", "全局唯一，文件内不得重复", "作为匹配键，不覆盖", "保留大小写；仅去除首尾空格"],
  ["用户", "姓名", "是", "—", "覆盖", "姓名不是唯一键"],
  ["用户", "科室编码", "是", "必须引用已存在科室", "覆盖当前科室", "变化时记录调科日志"],
  ["用户", "工号", "否", "建议文件内不重复", "覆盖", "若医院可保证唯一，系统可增加唯一校验"],
  ["用户", "手机号", "否", "格式校验", "覆盖", "按权限脱敏显示"],
  ["用户", "登录账号", "否", "如填写建议唯一", "覆盖", "是否需要取决于登录认证方式"],
  ["用户", "状态", "是", "启用/停用", "覆盖", "停用后禁止登录和企微推送"],
  ["角色", "角色", "不导入", "由管理员配置", "保持系统现有值", "新用户默认无角色"],
  ["历史数据", "业务记录", "不导入", "不得覆盖", "保持原值", "人员调科不修改历史科室快照"],
];
body(dict, "A5:F18");
dict.getRange("A5:F18").format.fill = PALE;
dict.getRange("A20:F20").values = [["校验场景", "判断条件", "结果", "处理方式", "示例", "审计要求"]];
header(dict, "A20:F20");
dict.getRange("A21:F30").values = [
  ["企微ID重复", "同一文件出现两次", "失败", "两行均拒绝或要求修正后重传", "zhangsan重复", "记录失败原因"],
  ["企微ID已存在", "数据库已存在", "更新", "更新允许覆盖字段，不修改角色", "zhangsan", "记录字段前后值"],
  ["企微ID变化", "旧用户需改为新ID", "禁止普通导入", "管理员执行身份迁移", "old→new", "保留全部历史"],
  ["科室不存在", "用户引用未知科室编码", "失败", "拒绝该用户行", "KF999", "记录引用错误"],
  ["用户调科", "已存在用户的科室编码变化", "警告并更新", "预检展示原科室和新科室", "KF001→KF002", "写入调科日志"],
  ["用户无角色", "新用户导入成功", "待配置", "禁止进入业务模块", "新用户", "记录管理员授权"],
  ["文件中缺席", "历史用户未出现在本批文件", "不处理", "保持原状态", "—", "增量导入"],
  ["用户停用", "状态=停用", "更新", "禁止登录和消息推送", "停用", "保留历史业务记录"],
  ["科室停用", "科室仍有启用用户", "警告/阻止", "先处理用户归属", "—", "记录处理人"],
  ["密码字段", "文件中出现密码信息", "拒绝", "不导入密码", "—", "记录安全异常"],
];
body(dict, "A21:F30");
dict.getRange("A:F").format.columnWidth = 20;
dict.getRange("B:B").format.columnWidth = 26;
dict.getRange("D:D").format.columnWidth = 34;
dict.getRange("F:F").format.columnWidth = 32;
dict.freezePanes.freezeRows(4);

for (const s of [dept, user]) s.getRange("A4:Z4").format.rowHeight = 32;

const checks = [];
for (const [sheetName, range] of [
  ["使用说明", "A1:G23"],
  ["科室导入模板", "A1:G10"],
  ["用户导入模板", "A1:H10"],
  ["字段与校验规则", "A1:F30"],
]) {
  const inspection = await wb.inspect({ kind: "table", range: `${sheetName}!${range}`, include: "values,formulas", tableMaxRows: 35, tableMaxCols: 10, maxChars: 6000 });
  checks.push(inspection.ndjson);
  const preview = await wb.render({ sheetName, range, scale: 1.2, format: "png" });
  await fs.writeFile(`${outputDir}/preview_${sheetName}.png`, new Uint8Array(await preview.arrayBuffer()));
}
const errors = await wb.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options: { useRegex: true, maxResults: 100 }, summary: "formula error scan" });
const xlsx = await SpreadsheetFile.exportXlsx(wb);
const outputPath = `${outputDir}/康复医院_科室用户导入模板_单科室版.xlsx`;
await xlsx.save(outputPath);
await fs.writeFile(`${outputDir}/verification.txt`, checks.join("\n---\n") + "\nERRORS\n" + errors.ndjson, "utf8");
console.log(outputPath);
