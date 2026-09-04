import fs from "node:fs/promises";
import { Workbook, SpreadsheetFile } from "@oai/artifact-tool";

const outputDir = "D:/projects/kangfuhospital/outputs/user_dept_import_20260827";
await fs.mkdir(outputDir, { recursive: true });

const wb = Workbook.create();
const guide = wb.worksheets.add("使用说明");
const dept = wb.worksheets.add("科室导入模板");
const user = wb.worksheets.add("用户导入模板");
const rel = wb.worksheets.add("用户科室关系模板");
const dict = wb.worksheets.add("数据字典");

const navy = "#1F4E78";
const blue = "#D9EAF7";
const pale = "#F4F8FB";
const yellow = "#FFF2CC";
const red = "#FCE8E6";
const green = "#E2F0D9";
const border = "#D9E2F3";
const font = "Microsoft YaHei";

function baseSheet(sheet) {
  sheet.showGridLines = false;
  sheet.getRange("A1:Z200").format.font = { name: font, size: 10, color: "#222222" };
}

function styleHeader(sheet, range) {
  sheet.getRange(range).format = {
    fill: navy,
    font: { name: font, size: 10, bold: true, color: "#FFFFFF" },
    verticalAlignment: "center",
    horizontalAlignment: "center",
    wrapText: true,
    borders: { preset: "all", style: "thin", color: border },
  };
}

function styleData(sheet, range) {
  sheet.getRange(range).format = {
    verticalAlignment: "center",
    wrapText: true,
    borders: { preset: "inside", style: "thin", color: "#E6EAF0" },
  };
}

function addTitle(sheet, title, subtitle, lastCol) {
  sheet.getRange(`A1:${lastCol}1`).merge();
  sheet.getRange("A1").values = [[title]];
  sheet.getRange("A1").format = {
    fill: navy,
    font: { name: font, size: 18, bold: true, color: "#FFFFFF" },
    horizontalAlignment: "left",
    verticalAlignment: "center",
  };
  sheet.getRange("A1").format.rowHeight = 34;
  sheet.getRange(`A2:${lastCol}2`).merge();
  sheet.getRange("A2").values = [[subtitle]];
  sheet.getRange("A2").format = {
    fill: pale,
    font: { name: font, size: 10, color: "#44546A" },
    wrapText: true,
    verticalAlignment: "center",
  };
  sheet.getRange("A2").format.rowHeight = 30;
}

[guide, dept, user, rel, dict].forEach(baseSheet);

// 使用说明
addTitle(guide, "康复医院｜科室与用户导入模板", "设计原则：企微ID唯一识别用户，科室编码唯一识别科室；一人多科室通过独立关系表表达。", "G");
guide.getRange("B4:G4").merge();
guide.getRange("A4").values = [["项目"]];
guide.getRange("B4").values = [["规则"]];
styleHeader(guide, "A4:G4");
const guideRules = [
  ["推荐导入顺序", "1. 科室导入模板 → 2. 用户导入模板 → 3. 用户科室关系模板"],
  ["用户唯一键", "企微ID。去除首尾空格后按原值匹配，不转换大小写；一经建立不允许普通导入修改。"],
  ["科室唯一键", "科室编码。科室改名时按编码更新名称，不生成新科室。"],
  ["用户与科室", "支持多对多；同一用户只能有一个主科室，但可以有多个兼职/管理科室。"],
  ["导入模式", "增量导入：文件中存在则新增或更新；文件中未出现的记录保持不变，不自动停用或删除。"],
  ["删除策略", "不允许物理删除已被业务数据引用的用户、科室或关系；通过“状态=停用”失效。"],
  ["覆盖规则", "基础字段可更新；系统内已有操作历史、业务表单、推送记录不得被覆盖。"],
  ["角色和数据范围", "在用户科室关系中维护，支持同一用户在不同科室拥有不同角色。"],
  ["企微推送", "只有企微ID有效且状态启用的用户才可进入推送接收人范围。"],
  ["示例数据", "模板中的示例均为虚构数据。正式导入前可删除示例行，禁止使用真实患者信息。"],
];
for (let i = 0; i < guideRules.length; i++) {
  const row = 5 + i;
  guide.getRange(`B${row}:G${row}`).merge();
  guide.getRange(`A${row}`).values = [[guideRules[i][0]]];
  guide.getRange(`B${row}`).values = [[guideRules[i][1]]];
}
styleData(guide, "A5:G14");
guide.getRange("A16:G16").merge();
guide.getRange("A16").values = [["导入处理结果建议"]];
guide.getRange("A16").format = { fill: blue, font: { name: font, bold: true, color: navy }, verticalAlignment: "center" };
guide.getRange("A17:G21").values = [
  ["结果", "新增", "更新", "无变化", "失败", "警告", "处理建议"],
  ["含义", "唯一键不存在", "唯一键已存在且字段变化", "数据完全一致", "必填、引用或格式校验不通过", "可导入但需关注", "导入后提供批次号和失败明细下载"],
  ["事务建议", "", "", "", "单行失败不影响其他合格行", "", "支持整批撤销时必须检查导入后的人工修改"],
  ["审计内容", "", "", "", "", "", "原文件、导入人、时间、来源IP、字段变更前后值"],
  ["正式执行", "", "", "", "", "", "先预检并展示统计，用户确认后再执行"],
];
styleHeader(guide, "A17:G17");
styleData(guide, "A18:G21");
guide.getRange("A23:G23").merge();
guide.getRange("A23").values = [["注意：企微ID适合作为系统外部身份唯一键，但若企业微信账号被删除后重建、企微ID发生变化，应由管理员执行“身份迁移”，不能通过普通导入直接替换，否则可能把历史记录拆成两个用户。"]];
guide.getRange("A23").format = { fill: red, font: { name: font, bold: true, color: "#9C0006" }, wrapText: true, verticalAlignment: "center" };
guide.getRange("A23").format.rowHeight = 45;
guide.getRange("A17:F21").format.columnWidth = 13;
guide.getRange("G17:G21").format.columnWidth = 34;
guide.freezePanes.freezeRows(4);

// 科室模板
addTitle(dept, "科室导入模板", "必填列以“*”标识。先导入父级科室，再导入下级科室；正式文件请保留表头。", "G");
const deptHeaders = ["科室编码*", "科室名称*", "上级科室编码", "科室类型*", "状态*", "排序号", "备注"];
dept.getRange("A4:G4").values = [deptHeaders];
styleHeader(dept, "A4:G4");
dept.getRange("A5:G7").values = [
  ["KF001", "康复医学中心", "", "临床科室", "启用", 10, "示例：一级科室"],
  ["KF00101", "神经重症康复病房", "KF001", "病区", "启用", 20, "示例：下级病区"],
  ["YYB", "运营部", "", "职能科室", "启用", 30, "示例：职能科室"],
];
dept.getRange("A5:G103").format.fill = "#FFF8E1";
dept.getRange("A5:G7").format.fill = yellow;
styleData(dept, "A5:G103");
dept.getRange("A5:C103").format.numberFormat = "@";
dept.getRange("D5:D103").dataValidation = { rule: { type: "list", values: ["临床科室", "病区", "职能科室", "其他"] } };
dept.getRange("E5:E103").dataValidation = { rule: { type: "list", values: ["启用", "停用"] } };
dept.getRange("F5:F103").dataValidation = { rule: { type: "whole", operator: "between", formula1: 0, formula2: 999999 } };
dept.freezePanes.freezeRows(4);
dept.tables.add("A4:G103", true, "DepartmentImportTable");
dept.getRange("A:G").format.columnWidth = 17;
dept.getRange("B:B").format.columnWidth = 28;
dept.getRange("G:G").format.columnWidth = 32;

// 用户模板
addTitle(user, "用户导入模板", "企微ID是用户唯一识别码；用户与科室、角色的对应关系请在“用户科室关系模板”中导入。", "G");
const userHeaders = ["企微ID*", "姓名*", "工号", "手机号", "登录账号", "状态*", "备注"];
user.getRange("A4:G4").values = [userHeaders];
styleHeader(user, "A4:G4");
user.getRange("A5:G7").values = [
  ["zhangsan", "张三", "YS0101", "13800000001", "zhangsan", "启用", "示例：主管医生"],
  ["lisi", "李四", "HS0201", "13800000002", "lisi", "启用", "示例：护士"],
  ["wangwu", "王五", "YY0031", "13800000003", "wangwu", "启用", "示例：运营人员"],
];
user.getRange("A5:G203").format.fill = "#FFF8E1";
user.getRange("A5:G7").format.fill = yellow;
styleData(user, "A5:G203");
user.getRange("A5:E203").format.numberFormat = "@";
user.getRange("F5:F203").dataValidation = { rule: { type: "list", values: ["启用", "停用"] } };
user.freezePanes.freezeRows(4);
user.tables.add("A4:G203", true, "UserImportTable");
user.getRange("A:A").format.columnWidth = 23;
user.getRange("B:B").format.columnWidth = 14;
user.getRange("C:E").format.columnWidth = 18;
user.getRange("F:F").format.columnWidth = 12;
user.getRange("G:G").format.columnWidth = 32;

// 用户科室关系模板
addTitle(rel, "用户科室关系导入模板", "关系唯一键：企微ID + 科室编码 + 角色编码。一个用户可以有多行关系，但只能有一行“是否主科室=是”。", "I");
const relHeaders = ["企微ID*", "科室编码*", "是否主科室*", "角色编码*", "数据范围*", "状态*", "生效日期", "失效日期", "备注"];
rel.getRange("A4:I4").values = [relHeaders];
styleHeader(rel, "A4:I4");
rel.getRange("A5:I8").values = [
  ["zhangsan", "KF00101", "是", "DOCTOR", "本人患者", "启用", new Date("2026-08-01"), null, "示例：主科室"],
  ["zhangsan", "KF001", "否", "DEPT_DIRECTOR", "本科室", "启用", new Date("2026-08-01"), null, "示例：兼任中心管理角色"],
  ["lisi", "KF00101", "是", "NURSE", "本科室", "启用", new Date("2026-08-01"), null, "示例：护士"],
  ["wangwu", "YYB", "是", "OPERATION", "全院", "启用", new Date("2026-08-01"), null, "示例：运营人员"],
];
rel.getRange("A5:I303").format.fill = "#FFF8E1";
rel.getRange("A5:I8").format.fill = yellow;
styleData(rel, "A5:I303");
rel.getRange("A5:B303").format.numberFormat = "@";
rel.getRange("C5:C303").dataValidation = { rule: { type: "list", values: ["是", "否"] } };
rel.getRange("D5:D303").dataValidation = { rule: { type: "list", formula1: "'数据字典'!$B$5:$B$16" } };
rel.getRange("E5:E303").dataValidation = { rule: { type: "list", values: ["全院", "本科室", "本人患者"] } };
rel.getRange("F5:F303").dataValidation = { rule: { type: "list", values: ["启用", "停用"] } };
rel.getRange("G5:H303").format.numberFormat = "yyyy-mm-dd";
rel.freezePanes.freezeRows(4);
rel.tables.add("A4:I303", true, "UserDepartmentRelationTable");
rel.getRange("A:B").format.columnWidth = 20;
rel.getRange("C:F").format.columnWidth = 17;
rel.getRange("G:H").format.columnWidth = 15;
rel.getRange("I:I").format.columnWidth = 34;

// 数据字典
addTitle(dict, "数据字典与校验规则", "字段编码和枚举值应在开发前由业务负责人确认；以下为建议初稿。", "F");
dict.getRange("A4:F4").values = [["字典类型", "编码/值", "显示名称", "适用数据范围", "是否允许导入", "说明"]];
styleHeader(dict, "A4:F4");
const roleRows = [
  ["角色", "SYS_ADMIN", "系统管理员", "全院", "是", "系统管理及全部业务权限"],
  ["角色", "FINANCE", "财务科", "全院", "是", "欠费导入、处理及推送"],
  ["角色", "LEGAL", "法务", "全院", "是", "诉讼相关追缴处理"],
  ["角色", "OPERATION", "运营部", "全院", "是", "预出院运营与督办"],
  ["角色", "INSURANCE", "医保办", "全院", "是", "预出院查看与导出"],
  ["角色", "DEPT_DIRECTOR", "科主任", "本科室", "是", "管理本科室患者"],
  ["角色", "DOCTOR", "主管医生", "本人患者/本科室", "是", "预计出院及复诊填报"],
  ["角色", "NURSE", "护士", "本科室", "是", "护理与随访填报"],
  ["角色", "OUTPATIENT", "门诊部", "全院", "是", "复诊到诊跟踪"],
  ["角色", "NUTRITION", "营养科", "全院", "是", "营养会诊填报"],
  ["角色", "HOME_REHAB", "居家康复科", "全院", "是", "居家康复填报"],
  ["角色", "NURSING_ADMIN", "护理部", "全院", "是", "院后回访管理"],
];
dict.getRange("A5:F16").values = roleRows;
dict.getRange("A18:F18").values = [["校验对象", "校验规则", "错误级别", "处理方式", "示例", "备注"]];
styleHeader(dict, "A18:F18");
dict.getRange("A19:F30").values = [
  ["企微ID", "必填；文件内不得重复；用户表中全局唯一", "失败", "拒绝该行", "zhangsan", "不转大小写"],
  ["企微ID变更", "普通导入不允许修改已有身份", "失败", "管理员走身份迁移", "旧ID→新ID", "保留全部历史关联"],
  ["科室编码", "必填；全局唯一", "失败", "拒绝该行", "KF00101", "按编码更新名称"],
  ["上级科室", "必须已存在；不能引用自身或形成循环", "失败", "拒绝该行", "KF001", "父级优先导入"],
  ["用户引用", "关系表中的企微ID必须存在且启用", "失败", "拒绝该行", "zhangsan", "先导入用户"],
  ["科室引用", "关系表中的科室编码必须存在", "失败", "拒绝该行", "KF00101", "先导入科室"],
  ["主科室", "同一企微ID最多一条启用的主科室关系", "失败", "拒绝冲突行", "是", "停用关系不计"],
  ["关系重复", "企微ID+科室编码+角色编码唯一", "失败", "更新原关系", "三字段组合", "不得新增重复关系"],
  ["失效日期", "不得早于生效日期", "失败", "拒绝该行", "2026-12-31", "空值表示长期有效"],
  ["状态", "仅允许启用/停用", "失败", "拒绝该行", "启用", "不允许直接删除"],
  ["用户缺席", "未出现在本批文件中的用户保持原状态", "提示", "不处理", "—", "默认增量导入"],
  ["敏感信息", "模板仅包含工作人员信息，不得包含患者信息或密码", "失败", "拒绝并记录", "—", "手机号按权限展示"],
];
styleData(dict, "A5:F16");
styleData(dict, "A19:F30");
dict.getRange("A5:F16").format.fill = pale;
dict.getRange("A:F").format.columnWidth = 20;
dict.getRange("C:C").format.columnWidth = 22;
dict.getRange("D:D").format.columnWidth = 22;
dict.getRange("F:F").format.columnWidth = 34;
dict.freezePanes.freezeRows(4);

// Common presentation settings
for (const sheet of [dept, user, rel]) {
  sheet.getRange("A4:Z4").format.rowHeight = 32;
}

const inspections = [];
for (const [sheetName, range] of [
  ["使用说明", "A1:G23"],
  ["科室导入模板", "A1:G10"],
  ["用户导入模板", "A1:G10"],
  ["用户科室关系模板", "A1:I11"],
  ["数据字典", "A1:F30"],
]) {
  const result = await wb.inspect({ kind: "table", range: `${sheetName}!${range}`, include: "values,formulas", tableMaxRows: 35, tableMaxCols: 10, maxChars: 5000 });
  inspections.push(result.ndjson);
  const preview = await wb.render({ sheetName, range, scale: 1.2, format: "png" });
  await fs.writeFile(`${outputDir}/preview_${sheetName}.png`, new Uint8Array(await preview.arrayBuffer()));
}

const errors = await wb.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 100 },
  summary: "formula error scan",
});

const output = await SpreadsheetFile.exportXlsx(wb);
const outputPath = `${outputDir}/康复医院_科室用户导入模板.xlsx`;
await output.save(outputPath);
await fs.writeFile(`${outputDir}/verification.txt`, inspections.join("\n---\n") + "\nERRORS\n" + errors.ndjson, "utf8");
console.log(outputPath);
