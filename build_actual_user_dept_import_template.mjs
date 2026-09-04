import fs from "node:fs/promises";
import { Workbook, SpreadsheetFile } from "@oai/artifact-tool";

const outputDir = "D:/projects/kangfuhospital/outputs/actual_user_dept_import_20260904";
const outputPath = `${outputDir}/康复医院_科室用户导入模板_实际环境版.xlsx`;
const wb = Workbook.create();
const guide = wb.worksheets.add("填报说明");
const dept = wb.worksheets.add("科室导入");
const user = wb.worksheets.add("用户导入");
const snapshot = wb.worksheets.add("当前环境参考");

const FONT = "Arial";
const NAVY = "#1F4E78";
const BLUE = "#D9EAF7";
const INPUT = "#FFF2CC";
const SAMPLE = "#E2F0D9";
const LIGHT = "#F3F6F9";
const TEXT = "#1F2937";
const BORDER = "#D7DEE7";

function base(sheet) {
  sheet.showGridLines = false;
  sheet.getRange("A1:K220").format.font = { name: FONT, size: 10, color: TEXT };
  sheet.getRange("A1:K220").format.verticalAlignment = "center";
}
function header(sheet, range) {
  const r = sheet.getRange(range);
  r.format = { fill: NAVY, font: { name: FONT, size: 10, bold: true, color: "#FFFFFF" }, horizontalAlignment: "center", verticalAlignment: "center", wrapText: true };
  r.format.borders = { preset: "all", style: "thin", color: "#FFFFFF" };
  r.format.rowHeight = 30;
}
function section(sheet, cellRange, text) {
  sheet.getRange(cellRange).merge();
  sheet.getRange(cellRange.split(":")[0]).values = [[text]];
  sheet.getRange(cellRange).format = { fill: BLUE, font: { name: FONT, bold: true, color: NAVY }, borders: { preset: "outside", style: "thin", color: BORDER } };
}
[guide, dept, user, snapshot].forEach(base);

guide.getRange("A1:F1").merge();
guide.getRange("A1").values = [["康复医院科室与用户导入模板"]];
guide.getRange("A1:F1").format = { font: { name: FONT, size: 16, bold: true, color: NAVY }, rowHeight: 30 };
guide.getRange("A2:F2").merge();
guide.getRange("A2").values = [["依据 2026-09-04 当前运行环境、数据库字段和管理页面生成"]];
guide.getRange("A2:F2").format.font = { name: FONT, size: 10, italic: true, color: "#5B6573" };
section(guide, "A4:F4", "填报与导入顺序");
guide.getRange("A5:B12").values = [
  ["步骤", "要求"],
  ["1", "先维护“科室导入”，再维护“用户导入”。"],
  ["2", "导入页第1行为固定表头，不要改名、合并或新增标题行。"],
  ["3", "第2行为绿色示例，正式导入前删除；从第2行开始填写正式数据。"],
  ["4", "黄色区域为可填写区域。编码、工号、企微ID均按文本保存，避免前导零丢失。"],
  ["5", "科室编码、工号、企微ID必须唯一；用户科室编码必须存在且处于启用状态。"],
  ["6", "登录名不填写，由系统根据姓名拼音生成；发生重名时自动追加数字。"],
  ["7", "角色不在本模板导入。用户建立后由管理员在系统中单独分配角色。"]
];
header(guide, "A5:B5");
guide.getRange("A6:B12").format.borders = { preset: "inside", style: "thin", color: BORDER };
guide.getRange("B6:B12").format.wrapText = true;
section(guide, "A14:F14", "当前系统约束");
guide.getRange("A15:C22").values = [
  ["对象", "字段", "实际约束"],
  ["科室", "科室编码", "必填，最长64字符，全局唯一；系统新建时自动去除首尾空格并转为大写。"],
  ["科室", "科室名称", "必填，最长128字符。"],
  ["用户", "姓名", "必填，最长128字符。"],
  ["用户", "工号", "必填，最长64字符，全局唯一；主管医生匹配使用该字段。"],
  ["用户", "企微ID", "必填，最长128字符，全局唯一；企微推送使用该字段。"],
  ["用户", "所属科室", "当前业务用户必须归属一个启用科室；系统管理员账号为例外。"],
  ["用户", "登录名与角色", "登录名由系统生成；初始角色为空，之后由管理员分配。"]
];
header(guide, "A15:C15");
guide.getRange("A16:C22").format.borders = { preset: "inside", style: "thin", color: BORDER };
guide.getRange("C16:C22").format.wrapText = true;
guide.getRange("A24:F26").merge();
guide.getRange("A24").values = [["注意：当前运行版本的管理页面提供单条新增、启停和角色分配；代码中尚未发现科室/用户批量导入接口。本文件已按当前数据模型收敛字段，可用于数据收集及后续导入功能对接；正式上线批量导入前需让后端按第1行表头完成映射。"]];
guide.getRange("A24:F26").format = { fill: "#FCE4D6", font: { name: FONT, color: "#9C3B14" }, wrapText: true, verticalAlignment: "center", borders: { preset: "outside", style: "thin", color: "#F4B183" } };
guide.getRange("A:A").format.columnWidth = 14;
guide.getRange("B:B").format.columnWidth = 68;
guide.getRange("C:C").format.columnWidth = 76;
guide.getRange("D:F").format.columnWidth = 12;
guide.getRange("A5:C22").format.rowHeight = 28;

dept.getRange("A1:B1").values = [["科室编码*", "科室名称*"]];
header(dept, "A1:B1");
dept.getRange("A2:B2").values = [["REHAB_EXAMPLE", "示例康复科（正式导入前删除本行）"]];
dept.getRange("A2:B2").format.fill = SAMPLE;
dept.getRange("A3:B202").format.fill = INPUT;
dept.getRange("A2:B202").format.borders = { preset: "inside", style: "thin", color: BORDER };
dept.getRange("A2:A202").format.numberFormat = "@";
dept.getRange("A:A").format.columnWidth = 26;
dept.getRange("B:B").format.columnWidth = 42;
dept.freezePanes.freezeRows(1);

user.getRange("A1:D1").values = [["姓名*", "工号*", "企微ID*", "科室编码*"]];
header(user, "A1:D1");
user.getRange("A2:D2").values = [["张三", "EMP00123", "zhangsan", "REHAB_EXAMPLE"]];
user.getRange("A2:D2").format.fill = SAMPLE;
user.getRange("A3:D502").format.fill = INPUT;
user.getRange("A2:D502").format.borders = { preset: "inside", style: "thin", color: BORDER };
user.getRange("A2:D502").format.numberFormat = "@";
user.getRange("A:A").format.columnWidth = 18;
user.getRange("B:B").format.columnWidth = 18;
user.getRange("C:C").format.columnWidth = 28;
user.getRange("D:D").format.columnWidth = 28;
user.freezePanes.freezeRows(1);

snapshot.getRange("A1:F1").merge();
snapshot.getRange("A1").values = [["当前运行环境参考"]];
snapshot.getRange("A1:F1").format.font = { name: FONT, size: 16, bold: true, color: NAVY };
snapshot.getRange("A2:F2").merge();
snapshot.getRange("A2").values = [["读取时间：2026-09-04 10:19（Asia/Shanghai）  来源：http://localhost:8080/api/system/departments、/api/system/users、/api/system/roles"]];
snapshot.getRange("A2:F2").format = { font: { name: FONT, size: 9, italic: true, color: "#5B6573" }, wrapText: true };
section(snapshot, "A4:F4", "现有科室（导入用户时优先使用以下编码）");
snapshot.getRange("A5:C15").values = [
  ["科室编码", "科室名称", "状态"],
  ["BONE_PAIN_REHAB", "骨与疼痛康复病房", "启用"],
  ["DEV-ARR-01", "神经重症康复病房", "启用"],
  ["DEV-ARR-02", "神经高压氧康复病房", "启用"],
  ["DEV-ARR-03", "重症康复病房", "启用"],
  ["DEV-ARR-04", "神经损伤康复病房", "启用"],
  ["DEV-ARR-05", "老年医学康复病房", "启用"],
  ["DEV-ARR-06", "骨与关节病运动康复病房", "启用"],
  ["DEV-HOME-REHAB", "居家康复科", "启用"],
  ["DEV-NEURO-ICU", "神经重症康复", "启用"],
  ["DEV-NUTRITION", "营养科", "启用"]
];
header(snapshot, "A5:C5");
snapshot.getRange("A6:C15").format.borders = { preset: "inside", style: "thin", color: BORDER };
section(snapshot, "A17:F17", "现有用户唯一值（新增前用于查重）");
snapshot.getRange("A18:F24").values = [
  ["登录名", "姓名", "工号", "企微ID", "所属科室", "角色"],
  ["dev_niewenbin", "聂文斌", 3563, "niewenbin", "神经重症康复", "随访员"],
  ["dev_pengbaicheng", "彭百成", 4673, "pengbaicheng", "神经重症康复", "科主任"],
  ["dev_wangbingxin1", "王冰芯", 5225, "wangbingxin1", "神经重症康复", "主管医生"],
  ["dev_luqian", "卢倩", 5293, "luqian", "营养科", "营养科"],
  ["dev_gaoying", "高颖", 5277, "gaoying", "居家康复科", "居家康复科、系统管理员"],
  ["admin", "系统管理员", "ADMIN", "admin", "", "系统管理员"]
];
header(snapshot, "A18:F18");
snapshot.getRange("A19:F24").format.borders = { preset: "inside", style: "thin", color: BORDER };
snapshot.getRange("A19:F24").format.numberFormat = "@";
snapshot.getRange("C19:C23").format.numberFormat = "00000";
section(snapshot, "A26:F26", "内置角色（不在用户导入表填写）");
snapshot.getRange("A27:B39").values = [
  ["角色编码", "角色名称"],
  ["OPERATIONS", "运营部"], ["ATTENDING_DOCTOR", "主管医生"], ["OUTPATIENT", "门诊部"],
  ["NUTRITION", "营养科"], ["HOME_REHAB", "居家康复科"], ["SYSTEM_ADMIN", "系统管理员"],
  ["FINANCE", "财务科"], ["LEGAL", "法务部"], ["MEDICAL_INSURANCE", "医保办"],
  ["DEPARTMENT_DIRECTOR", "科主任"], ["BED_MANAGER", "床位管理员"], ["FOLLOW_UP", "随访员"]
];
header(snapshot, "A27:B27");
snapshot.getRange("A28:B39").format.borders = { preset: "inside", style: "thin", color: BORDER };
snapshot.getRange("A:A").format.columnWidth = 28;
snapshot.getRange("B:B").format.columnWidth = 34;
snapshot.getRange("C:D").format.columnWidth = 20;
snapshot.getRange("E:E").format.columnWidth = 28;
snapshot.getRange("F:F").format.columnWidth = 34;
snapshot.freezePanes.freezeRows(2);

// Useful input validation based on current database limits.
dept.getRange("A2:A202").dataValidation = { rule: { type: "textLength", operator: "between", formula1: 1, formula2: 64 } };
dept.getRange("B2:B202").dataValidation = { rule: { type: "textLength", operator: "between", formula1: 1, formula2: 128 } };
user.getRange("A2:A502").dataValidation = { rule: { type: "textLength", operator: "between", formula1: 1, formula2: 128 } };
user.getRange("B2:B502").dataValidation = { rule: { type: "textLength", operator: "between", formula1: 1, formula2: 64 } };
user.getRange("C2:C502").dataValidation = { rule: { type: "textLength", operator: "between", formula1: 1, formula2: 128 } };
user.getRange("D2:D502").dataValidation = { rule: { type: "textLength", operator: "between", formula1: 1, formula2: 64 } };

await fs.mkdir(outputDir, { recursive: true });
for (const [sheetName, range, file] of [
  ["填报说明", "A1:F26", "guide.png"], ["科室导入", "A1:B12", "dept.png"],
  ["用户导入", "A1:D12", "user.png"], ["当前环境参考", "A1:F39", "snapshot.png"]
]) {
  const image = await wb.render({ sheetName, range, scale: 1.4, format: "png" });
  await fs.writeFile(`${outputDir}/${file}`, new Uint8Array(await image.arrayBuffer()));
}
const check = await wb.inspect({ kind: "table", range: "当前环境参考!A1:F39", include: "values,formulas", tableMaxRows: 40, tableMaxCols: 6 });
console.log(check.ndjson);
const errors = await wb.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!", options: { useRegex: true, maxResults: 100 }, summary: "final formula error scan" });
console.log(errors.ndjson);
const out = await SpreadsheetFile.exportXlsx(wb);
await out.save(outputPath);
console.log(outputPath);
