/* ================= 演示数据 ================= */
const DEPTS = ["神经重症康复病房","重症康复病房","神经损伤康复病房","神经高压氧康复病房","骨与关节病运动康复病房","老年医学康复病房"];
const FEE_TYPES = ["城镇职工基本医疗保险","城乡居民基本医疗保险","异地医保持卡","自费","征地超转","医保身份待定","城市老年人医疗保险"];

/* 欠费明细（在院+出院合并展示，欠押金/欠费金额统一为“欠费金额”正数）
   追缴进度状态：未催缴 / 协商中 / 拒绝缴费 / 移交法务发起诉讼 / 已缴费 */
const ARREARS = [
 {no:"1002381",zy:2,name:"赵某某",dept:"神经重症康复病房",fee:"城镇职工基本医疗保险",type:"在院患者",doc:"孟凡*",inDate:"2026-06-12",outDate:"",total:496017.49,prepay:50000,due:248008.74,arrear:198008.74,reason:"家庭困难，筹款中",progress:"协商中",update:"2026-08-03 08:30",op:"孟凡*",
  history:[{time:"2026-08-02 15:40",by:"孟凡*",action:"追缴进度：协商中；补充欠费原因：家庭困难，筹款中"},{time:"2026-07-28 10:12",by:"财务科-张会计",action:"追缴进度：未催缴 → 协商中"}]},
 {no:"1002455",zy:1,name:"凌某某",dept:"重症康复病房",fee:"城乡居民基本医疗保险",type:"在院患者",doc:"饶志*",inDate:"2026-06-28",outDate:"",total:437763.04,prepay:190000,due:306434.13,arrear:116434.13,reason:"",progress:"未催缴",update:"2026-08-03 08:30",op:"—",history:[]},
 {no:"1002502",zy:3,name:"张某某",dept:"神经重症康复病房",fee:"异地医保持卡",type:"在院患者",doc:"王立*",inDate:"2026-07-09",outDate:"",total:156120.67,prepay:30000,due:109284.47,arrear:79284.47,reason:"异地结算延迟",progress:"协商中",update:"2026-08-03 08:30",op:"王立*",
  history:[{time:"2026-08-01 09:05",by:"王立*",action:"追缴进度：未催缴 → 协商中；欠费原因：异地结算延迟"}]},
 {no:"1002518",zy:1,name:"孙某某",dept:"神经损伤康复病房",fee:"自费",type:"在院患者",doc:"张倩*",inDate:"2026-07-15",outDate:"",total:124604.10,prepay:50000,due:124604.10,arrear:74604.10,reason:"家属对康复疗程费用有异议",progress:"拒绝缴费",update:"2026-08-03 08:30",op:"张倩*",
  history:[{time:"2026-08-01 16:47",by:"张倩*",action:"追缴进度：协商中 → 拒绝缴费；补充欠费原因"},{time:"2026-07-29 11:20",by:"财务科-张会计",action:"追缴进度：未催缴 → 协商中"}]},
 {no:"1002477",zy:2,name:"杨某某",dept:"神经高压氧康复病房",fee:"征地超转",type:"在院患者",doc:"艾则*",inDate:"2026-07-02",outDate:"",total:129890.47,prepay:20000,due:64945.24,arrear:44945.24,reason:"待征地补偿到账",progress:"协商中",update:"2026-08-03 08:30",op:"艾则*",
  history:[{time:"2026-07-30 08:55",by:"艾则*",action:"追缴进度：未催缴 → 协商中"}]},
 {no:"1002533",zy:1,name:"王某某",dept:"神经重症康复病房",fee:"医保身份待定",type:"在院患者",doc:"王雪*",inDate:"2026-07-21",outDate:"",total:63145.03,prepay:30000,due:63145.03,arrear:33145.03,reason:"医保身份核定中",progress:"协商中",update:"2026-08-03 08:30",op:"王雪*",
  history:[{time:"2026-08-02 10:31",by:"王雪*",action:"补充欠费原因：医保身份核定中"}]},
 {no:"1002561",zy:1,name:"刘某某",dept:"骨与关节病运动康复病房",fee:"自费",type:"在院患者",doc:"陈明*",inDate:"2026-07-25",outDate:"",total:48210.50,prepay:30000,due:48210.50,arrear:18210.50,reason:"",progress:"未催缴",update:"2026-08-03 08:30",op:"—",history:[]},
 {no:"1001890",zy:2,name:"杨某某",dept:"神经高压氧康复病房",fee:"自费",type:"出院已结算",doc:"李国*",inDate:"2026-04-11",outDate:"2026-07-30",total:561588.98,prepay:343732.87,due:561588.98,yb:0,gr:0,arrear:217856.11,reason:"自费金额超预算，家属拒绝补足",progress:"移交法务发起诉讼",update:"2026-08-03 08:30",op:"财务科-张会计",
  history:[{time:"2026-08-01 14:00",by:"财务科-张会计",action:"追缴进度：拒绝缴费 → 移交法务发起诉讼；材料已移交法务"},{time:"2026-07-25 09:40",by:"李国*",action:"追缴进度：协商中 → 拒绝缴费"}]},
 {no:"1001966",zy:1,name:"史某某",dept:"神经高压氧康复病房",fee:"异地医保人员",type:"出院未结算",doc:"赵青*",inDate:"2026-05-06",outDate:"2026-07-28",total:139340.11,prepay:40000,due:68890.76,yb:68890.76,gr:0,arrear:30449.35,reason:"",progress:"协商中",update:"2026-08-03 08:30",op:"赵青*",
  history:[{time:"2026-07-31 13:18",by:"赵青*",action:"追缴进度：未催缴 → 协商中"}]},
 {no:"1002103",zy:1,name:"张某某",dept:"重症康复病房",fee:"城市老年人医疗保险",type:"出院未结算",doc:"殷秀*",inDate:"2026-05-16",outDate:"2026-07-28",total:454432.37,prepay:50000,due:231426.83,yb:231426.83,gr:0,arrear:173005.54,reason:"家属对费用有异议",progress:"拒绝缴费",update:"2026-08-03 08:30",op:"殷秀*",
  history:[{time:"2026-08-02 11:02",by:"殷秀*",action:"追缴进度：协商中 → 拒绝缴费"}]},
 {no:"1002239",zy:1,name:"赵某某",dept:"神经重症康复病房",fee:"城镇职工基本医疗保险",type:"出院已结算",doc:"张伟*",inDate:"2026-06-01",outDate:"2026-07-26",total:180012.58,prepay:30000,due:116958.15,yb:116958.15,gr:0,arrear:33054.43,reason:"",progress:"已缴费",update:"2026-08-03 08:30",op:"财务科-张会计",
  history:[{time:"2026-08-03 08:35",by:"财务科-张会计",action:"追缴进度：协商中 → 已缴费（财务核实到账）"}]},
 {no:"1002344",zy:3,name:"麦某某",dept:"神经高压氧康复病房",fee:"城乡居民基本医疗保险",type:"出院已结算",doc:"李楠*",inDate:"2026-02-06",outDate:"2026-05-06",total:214299.68,prepay:19000,due:176586.72,yb:176586.72,gr:0,arrear:18712.96,reason:"",progress:"协商中",update:"2026-08-03 08:30",op:"李楠*",
  history:[{time:"2026-07-29 15:26",by:"李楠*",action:"追缴进度：未催缴 → 协商中"}]},
 {no:"1002405",zy:1,name:"周某某",dept:"老年医学康复病房",fee:"公费医疗",type:"出院未结算",doc:"孙丽*",inDate:"2026-03-18",outDate:"2026-06-20",total:98760.20,prepay:60000,due:98760.20,yb:0,gr:0,arrear:38760.20,reason:"公费额度审批中",progress:"协商中",update:"2026-08-03 08:30",op:"孙丽*",
  history:[{time:"2026-07-30 17:08",by:"孙丽*",action:"补充欠费原因：公费额度审批中"}]},
];

const fmt0 = n => n.toLocaleString("zh-CN",{maximumFractionDigits:0});

/* 推送记录 */
const PUSH_LOG = [
 {time:"2026-08-03 09:00",channel:"企业微信",target:"神经重症康复病房-科主任、财务科、分管院领导",content:"欠费Top3通报（第32周）",status:"成功",by:"系统自动"},
 {time:"2026-08-03 09:00",channel:"企业微信",target:"神经高压氧康复病房-科主任、财务科、分管院领导",content:"欠费Top3通报（第32周）",status:"成功",by:"系统自动"},
 {time:"2026-08-03 09:00",channel:"企业微信",target:"重症康复病房-科主任、财务科、分管院领导",content:"欠费Top3通报（第32周）",status:"成功",by:"系统自动"},
 {time:"2026-07-27 09:00",channel:"企业微信",target:"神经重症康复病房-科主任、财务科、分管院领导",content:"欠费Top3通报（第31周）",status:"成功",by:"系统自动"},
 {time:"2026-07-21 15:32",channel:"企业微信",target:"运营部、医保办",content:"预计出院时间未填报名单",status:"成功",by:"系统自动"},
 {time:"2026-07-20 10:05",channel:"企业微信",target:"财务科、分管院领导",content:"欠费Top3通报（手动补发）",status:"成功",by:"财务科-张会计"},
];

/* 在院患者（预出院填报）  admitDays=入院天数 */
function fu(){ return {time:"",by:"",rec:"",remind:"是",hjEval:"满意",hjReason:"",hjAppt:"",nuEval:"满意",nuReason:"",nuAppt:""}; }
function follow3(){ return [fu(),fu(),fu()]; }
const INPATIENTS = [
 {no:"1002601",name:"陈某某",gender:"男",zy:1,dept:"神经重症康复病房",doc:"孟凡*",admitDays:3,admitDate:"2026-07-31",diag:"脑出血恢复期",planDate:"",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),reList:[],hjList:[],nuList:[]},
 {no:"1002598",name:"吴某某",gender:"女",zy:2,dept:"神经重症康复病房",doc:"王雪*",admitDays:5,admitDate:"2026-07-29",diag:"脊髓损伤康复期",planDate:"",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),reList:[],hjList:[],nuList:[]},
 {no:"1002590",reAppt:"2026-08-18",visit:{arrived:"是",time:"2026-08-18",by:"门诊部-陈静",reason:""},name:"郑某某",gender:"男",zy:1,dept:"重症康复病房",doc:"饶志*",admitDays:8,admitDate:"2026-07-26",diag:"脑梗死恢复期",planDate:"2026-08-15",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),
  reList:[{time:"2026-08-18",by:"饶志*",result:"已预约门诊"}],
  hjList:[{time:"2026-08-20",by:"居家康复组-刘*",result:"待上门评估"}],
  nuList:[{time:"2026-08-16",by:"营养科-周*",result:"已预约"}]},
 {no:"1002581",reAppt:"2026-08-13",visit:{arrived:"否",time:"",by:"门诊部-陈静",reason:"患者迁居外地，改为当地复诊"},name:"林某某",gender:"女",zy:1,dept:"神经损伤康复病房",doc:"张倩*",admitDays:12,admitDate:"2026-07-22",diag:"颅脑外伤恢复期",planDate:"2026-08-06",outDate:"",status:"正常",exReason:"",
  changes:[{from:"2026-08-10",to:"2026-08-06",reason:"恢复良好，提前出院",by:"张倩*",time:"2026-08-01 10:22"}],follow:follow3(),
  reList:[{time:"2026-08-13",by:"张倩*",result:"已预约门诊"}],hjList:[],nuList:[{time:"2026-08-07",by:"营养科-周*",result:"已预约"}]},
 {no:"1002570",reAppt:"2026-08-12",name:"黄某某",gender:"男",zy:3,dept:"神经高压氧康复病房",doc:"艾则*",admitDays:16,admitDate:"2026-07-18",diag:"一氧化碳中毒迟发脑病",planDate:"2026-08-05",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),
  reList:[{time:"2026-08-12",by:"艾则*",result:"已预约门诊"}],hjList:[{time:"2026-08-08",by:"居家康复组-刘*",result:"待上门评估"}],nuList:[]},
 {no:"1002555",name:"徐某某",gender:"女",zy:1,dept:"骨与关节病运动康复病房",doc:"陈明*",admitDays:9,admitDate:"2026-07-25",diag:"膝关节置换术后",planDate:"",outDate:"",status:"异常",exReason:"入院≥7天未填报预计出院时间",changes:[],follow:follow3(),reList:[],hjList:[],nuList:[]},
 {no:"1002540",reAppt:"2026-08-19",name:"何某某",gender:"男",zy:1,dept:"老年医学康复病房",doc:"孙丽*",admitDays:22,admitDate:"2026-07-12",diag:"帕金森病康复期",planDate:"2026-08-12",outDate:"",status:"正常",exReason:"",
  changes:[{from:"2026-08-08",to:"2026-08-12",reason:"肺部感染，延长治疗",by:"孙丽*",time:"2026-07-30 16:05"}],follow:follow3(),
  reList:[{time:"2026-08-19",by:"孙丽*",result:"已预约门诊"}],hjList:[],nuList:[{time:"2026-08-10",by:"营养科-周*",result:"已预约"},{time:"2026-08-14",by:"营养科-周*",result:"待会诊"}]},
 {no:"1002531",name:"高某某",gender:"女",zy:2,dept:"重症康复病房",doc:"殷秀*",admitDays:6,admitDate:"2026-07-28",diag:"重症肺炎后康复",planDate:"",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),reList:[],hjList:[],nuList:[],special:"是",specialReason:"终末期姑息治疗，无需院后服务预约"},
 {no:"1002520",reAppt:"2026-08-11",visit:{arrived:"是",time:"2026-08-11",by:"门诊部-刘敏",reason:""},name:"罗某某",gender:"男",zy:1,dept:"神经重症康复病房",doc:"孟凡*",admitDays:31,admitDate:"2026-07-03",diag:"脑干梗死恢复期",planDate:"2026-08-04",outDate:"",status:"正常",exReason:"",
  changes:[{from:"2026-08-02",to:"2026-08-04",reason:"家属接回时间调整",by:"孟凡*",time:"2026-07-28 09:12"}],follow:follow3(),
  reList:[{time:"2026-08-11",by:"孟凡*",result:"已预约门诊"}],
  hjList:[{time:"2026-08-06",by:"居家康复组-刘*",result:"待上门评估"},{time:"2026-08-13",by:"居家康复组-刘*",result:"待随访"}],
  nuList:[{time:"2026-08-05",by:"营养科-周*",result:"已预约"}]},
 {no:"1002509",reAppt:"2026-08-27",name:"宋某某",gender:"女",zy:1,dept:"神经高压氧康复病房",doc:"李国*",admitDays:18,admitDate:"2026-07-16",diag:"缺氧缺血性脑病",planDate:"2026-08-20",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),
  reList:[{time:"2026-08-27",by:"李国*",result:"已预约门诊"}],hjList:[],nuList:[]},
 {no:"1002499",reAppt:"2026-08-16",name:"唐某某",gender:"男",zy:4,dept:"神经损伤康复病房",doc:"张倩*",admitDays:45,admitDate:"2026-06-19",diag:"脊髓损伤后遗症",planDate:"2026-08-09",outDate:"",status:"异常",exReason:"预约随访时间未确认",changes:[],follow:follow3(),
  reList:[{time:"2026-08-16",by:"张倩*",result:"已预约门诊"}],hjList:[{time:"2026-08-11",by:"居家康复组-刘*",result:"待上门评估"}],nuList:[{time:"2026-08-12",by:"营养科-周*",result:"已预约"}]},
 {no:"1002488",name:"冯某某",gender:"女",zy:1,dept:"老年医学康复病房",doc:"孙丽*",admitDays:7,admitDate:"2026-07-27",diag:"股骨颈骨折术后",planDate:"",outDate:"",status:"正常",exReason:"",changes:[],follow:follow3(),reList:[],hjList:[],nuList:[]},
];
const latestTime = arr => arr && arr.length ? arr.map(x=>x.time).filter(Boolean).sort().slice(-1)[0] || "" : "";
const DISCHARGED = [
 {name:"钱某某",gender:"男",dept:"神经重症康复病房",doc:"孟凡*",outDate:"2026-07-20",follow:[
   {time:"2026-07-27",by:"随访护士-李晓",rec:"恢复良好",remind:"是",hjEval:"满意",hjReason:"",hjAppt:"2026-07-29",nuEval:"满意",nuReason:"",nuAppt:""},
   {time:"2026-08-02",by:"随访护士-李晓",rec:"一般",remind:"是",hjEval:"不满意",hjReason:"上门时间不固定",hjAppt:"2026-08-05",nuEval:"合格",nuReason:"",nuAppt:"2026-08-06"},
   fu()]},
 {name:"孙某某",gender:"女",dept:"老年医学康复病房",doc:"孙丽*",outDate:"2026-07-10",follow:[
   {time:"2026-07-17",by:"随访护士-王芳",rec:"恢复良好",remind:"是",hjEval:"满意",hjReason:"",hjAppt:"",nuEval:"满意",nuReason:"",nuAppt:""},
   {time:"2026-07-25",by:"随访护士-王芳",rec:"恢复良好",remind:"是",hjEval:"合格",hjReason:"",hjAppt:"2026-07-28",nuEval:"满意",nuReason:"",nuAppt:""},
   {time:"2026-08-01",by:"随访护士-王芳",rec:"恢复良好",remind:"否",hjEval:"满意",hjReason:"",hjAppt:"",nuEval:"满意",nuReason:"",nuAppt:""}]},
 {name:"周某某",gender:"男",dept:"骨与关节病运动康复病房",doc:"陈明*",outDate:"2026-07-25",follow:[
   {time:"2026-08-01",by:"随访护士-李晓",rec:"欠佳",remind:"是",hjEval:"不满意",hjReason:"训练强度过大，疼痛加重",hjAppt:"2026-08-04",nuEval:"合格",nuReason:"",nuAppt:"2026-08-03"},
   fu(),fu()]},
 {name:"吴某某",gender:"女",dept:"神经高压氧康复病房",doc:"艾则*",outDate:"2026-07-28",follow:[
   fu(),fu(),fu()]},
 {name:"郑某某",gender:"男",dept:"重症康复病房",doc:"饶志*",outDate:"2026-07-05",follow:[
   {time:"2026-07-12",by:"随访护士-王芳",rec:"恢复良好",remind:"是",hjEval:"满意",hjReason:"",hjAppt:"2026-07-15",nuEval:"满意",nuReason:"",nuAppt:""},
   {time:"2026-07-20",by:"随访护士-王芳",rec:"恢复良好",remind:"是",hjEval:"满意",hjReason:"",hjAppt:"",nuEval:"不满意",nuReason:"营养餐口味单一",nuAppt:"2026-07-23"},
   {time:"2026-08-02",by:"随访护士-王芳",rec:"恢复良好",remind:"否",hjEval:"满意",hjReason:"",hjAppt:"",nuEval:"合格",nuReason:"",nuAppt:""}]},
];

/* ================= 公共工具 ================= */
const fmt = n => n.toLocaleString("zh-CN",{minimumFractionDigits:2,maximumFractionDigits:2});
const fmtW = n => n>=10000 ? (n/10000).toFixed(2)+"万" : fmt(n);
const TODAY = "2026-08-03";

function getRole(){ return localStorage.getItem("proto_role") || "财务科（全院）"; }
function roleDept(){ const r=getRole(); return (r.includes("科主任")||r.includes("护士")) ? "神经重症康复病房" : null; }
function visibleRows(rows){ const d = roleDept(); return d ? rows.filter(r=>r.dept===d) : rows; }

function toast(msg){
  let t = document.querySelector(".toast");
  if(!t){ t=document.createElement("div"); t.className="toast"; document.body.appendChild(t); }
  t.textContent = msg; t.classList.add("show");
  setTimeout(()=>t.classList.remove("show"), 2200);
}

const NAV = [
 {group:"欠费管理", roles:["财务科","法务","科主任","护士","系统管理员"], items:[
   {key:"arrears", label:"欠费明细", href:"arrears.html"},
   {key:"report", label:"通报报表（Top榜）", href:"report.html"},
   {key:"pushlog", label:"推送记录", href:"pushlog.html"},
 ]},
 {group:"预出院管理", roles:["运营部","医保","科主任","护士","系统管理员"], items:[
   {key:"discharge", label:"预计出院管理", href:"discharge.html"},
   {key:"board", label:"统计分析", href:"board.html"},
 ]},
 {group:"系统管理", roles:["系统管理员"], items:[
   {key:"config", label:"费别系数配置", href:"config.html"},
   {key:"users", label:"用户管理", href:"users.html"},
   {key:"roles", label:"权限管理", href:"roles.html"},
 ]},
];

const ROLES_ALL = ["系统管理员（全院）","财务科（全院）","法务（全院）","运营部（全院）","医保办（全院）","科主任（本科室）","护士（本科室）"];

function typeTag(t){
  const map = {"在院患者":"blue","出院未结算":"orange","出院已结算":"green"};
  return `<span class="tag ${map[t]||'gray'}">${t}</span>`;
}


function renderLayout(activeKey, pageTitle){
  const role = getRole();
  const groups = NAV.filter(g => g.roles.some(r => role.includes(r)));
  const navHtml = groups.map(g =>
    `<div class="nav-group">${g.group}</div>` +
    g.items.map(i=>`<a class="nav-item ${i.key===activeKey?'active':''}" href="${i.href}">${i.label}</a>`).join("")
  ).join("");
  document.body.innerHTML = `
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">康复医院运营管理系统<small>欠费晾晒 · 预出院管理（原型）</small></div>
      ${navHtml}
    </aside>
    <div class="main">
      <div class="topbar">
        <div class="page-title">${pageTitle}</div>
        <div class="right">
          <span>当前角色：</span>
          <select id="roleSel" onchange="localStorage.setItem('proto_role',this.value);location.href=this.value.includes('运营部')||this.value.includes('医保')?'board.html':'arrears.html'">
            ${ROLES_ALL.map(r=>`<option ${r===getRole()?'selected':''}>${r}</option>`).join("")}
          </select>
          <a href="index.html" style="color:#2563eb">退出</a>
        </div>
      </div>
      <div class="content" id="content"></div>
    </div>
  </div>`;
  return document.getElementById("content");
}

function ribbon(){
  return `<div class="demo-ribbon">原型演示页面，所有数据均为模拟数据。字段口径：欠押金 = 预交金 − 应交押金（应交押金按费别比例计算）；出院欠费以医保结算后为准。数据通过报表导入更新（每日 08:30）。</div>`;
}

/* 医生姓名脱敏还原（欠费模块主管医生不加密显示） */
const DOC_FULL = {"孟凡*":"孟凡宇","饶志*":"饶志强","王立*":"王立军","张倩*":"张倩","艾则*":"艾则尔","王雪*":"王雪梅","陈明*":"陈明远","李国*":"李国栋","赵青*":"赵青禾","殷秀*":"殷秀兰","张伟*":"张伟","李楠*":"李楠","孙丽*":"孙丽娟"};
const fullDoc = d => DOC_FULL[d] || d;

/* 欠费通报文案生成 */
function buildNotice(){
  const agg = {};
  ARREARS.forEach(r=>{
    if(!agg[r.dept]) agg[r.dept] = {zy:0,ys:0,yj:0};
    if(r.type==="在院患者") agg[r.dept].zy += r.arrear;
    else if(r.type==="出院已结算") agg[r.dept].yj += r.arrear;
    else agg[r.dept].ys += r.arrear;
  });
  const rank = Object.entries(agg).map(([d,v])=>[d, v.zy+v.ys+v.yj, v]).sort((a,b)=>b[1]-a[1]);
  const total = ARREARS.reduce((s,r)=>s+r.arrear,0);
  let text = `【欠费通报 · 第32周】\n截至 2026-08-03 09:00，全院患者欠费合计 ${fmtW(total)} 元，欠费金额科室排名如下（欠费金额 = 在院患者欠费 + 出院已结算患者欠费 + 出院未结算患者欠费）：\n`;
  rank.forEach(([d,a,v],i)=>{ text += `${i+1}. ${d}：${fmtW(a)} 元（在院 ${fmtW(v.zy)} + 出院已结算 ${fmtW(v.yj)} + 出院未结算 ${fmtW(v.ys)}）\n`; });
  text += `请各位科室主任、主管医生及时关注本科室欠费患者，落实催缴。\n\n详情请点击康复医院运营管理系统查看（院内内网访问）：\nhttp://oa.kfyy.local/arrears`;
  return text;
}

/* ================= 页面：欠费明细 ================= */
function pageArrears(){
  const c = renderLayout("arrears","欠费明细");
  const rows = visibleRows(ARREARS);
  const total = rows.reduce((s,r)=>s+r.arrear,0);
  c.innerHTML = ribbon() + `
  <div class="stat-row">
    <div class="stat-card"><div class="label">欠费患者数</div><div class="value blue">${rows.length}</div><div class="extra">在院 ${rows.filter(r=>r.type==="在院患者").length} · 出院未结算 ${rows.filter(r=>r.type==="出院未结算").length} · 出院已结算 ${rows.filter(r=>r.type==="出院已结算").length}</div></div>
    <div class="stat-card"><div class="label">欠费金额合计</div><div class="value red">${fmtW(total)} 元</div><div class="extra">数据更新于 2026-08-03 08:30</div></div>
    <div class="stat-card"><div class="label">未催缴</div><div class="value">${rows.filter(r=>r.progress==="未催缴").length} 人</div><div class="extra">点击「编辑」维护追缴进度</div></div>
    <div class="stat-card"><div class="label">移交法务发起诉讼</div><div class="value">${rows.filter(r=>r.progress==="移交法务发起诉讼").length} 人</div><div class="extra">法务角色登录可查看并跟进</div></div>
  </div>
  <div class="card">
    <div class="filters">
      <select><option>全部欠费类型</option><option>在院患者</option><option>出院未结算</option><option>出院已结算</option></select>
      <select><option>全部科室</option>${DEPTS.map(d=>`<option>${d}</option>`).join("")}</select>
      <select><option>全部费别</option>${FEE_TYPES.map(d=>`<option>${d}</option>`).join("")}</select>
      <select><option>追缴进度（全部）</option><option>未催缴</option><option>协商中</option><option>拒绝缴费</option><option>移交法务发起诉讼</option><option>已缴费</option></select>
      <input placeholder="住院号 / 姓名 / 主管医生" style="width:180px">
      <button class="btn primary" onclick="toast('已按条件筛选（原型演示）')">查询</button>
      <div class="spacer"></div>
      <button class="btn" onclick="toast('已上传导入模板，更新 13 条，新增 0 条（原型演示）')">导入报表</button>
      <button class="btn" onclick="toast('已导出 Excel（原型演示）')">导出 Excel</button>
    </div>
  </div>
  <div class="card">
    <h3>欠费患者列表</h3>
    <div style="overflow-x:auto">
    <table class="grid">
      <thead><tr>
        <th>住院号</th><th class="num">住院次数</th><th>姓名</th><th>住院病区</th><th>费别</th><th>欠费类型</th><th>主管医生</th>
        <th>入区日期</th><th>出区日期</th>
        <th class="num">总费用(元)</th><th class="num">预交金(元)</th><th class="num">医保支付(元)</th><th class="num">个人账户支付(元)</th><th class="num">应交押金(元)</th><th class="num">欠费金额(元)</th>
        <th>欠费原因</th><th>追缴进度</th><th>最近操作人</th><th>数据更新时间</th><th>操作</th>
      </tr></thead>
      <tbody id="arrearBody">
      ${[...rows].sort((a,b)=>b.arrear-a.arrear).map((r,i)=>`
        <tr>
          <td>${r.no}</td><td class="num">${r.zy||1}</td><td>${r.name}</td><td>${r.dept}</td><td>${r.fee}</td>
          <td>${typeTag(r.type)}</td>
          <td>${fullDoc(r.doc)}</td>
          <td>${r.inDate||'—'}</td><td>${r.outDate||'<span style="color:#cbd5e1">未出区</span>'}</td>
          <td class="num">${fmt(r.total)}</td><td class="num">${fmt(r.prepay)}</td>
          ${r.type==="在院患者"?'<td class="num" style="color:#cbd5e1">—</td><td class="num" style="color:#cbd5e1">—</td>':`<td class="num">${fmt(r.yb||0)}</td><td class="num">${fmt(r.gr||0)}</td>`}
          <td class="num">${fmt(r.due)}</td>
          <td class="num money-neg">${fmt(r.arrear)}</td>
          <td>${r.reason||'<span style="color:#cbd5e1">—</span>'}</td>
          <td>${progressTag(r.progress)}</td>
          <td>${r.op}</td>
          <td style="color:#9ca3af">${r.update}</td>
          <td><button class="btn small" onclick='openArrearEdit("${r.no}")'>编辑</button></td>
        </tr>`).join("")}
      </tbody>
    </table>
    </div>
    <div class="note">医保支付、个人账户支付仅出院患者（已结算/未结算）展示，在院患者不展示。欠费金额随每日报表导入自动更新（按住院号匹配）；欠费原因、追缴进度由科室人工在「编辑」中填写维护，系统不做规则校验、不判断对错，但每次修改均记录操作人、操作时间与内容，全程留痕可追溯。</div>
  </div>
  <div class="modal-mask" id="arrearModal">
    <div class="modal" style="width:520px">
      <h3>编辑催缴信息</h3>
      <label>患者</label><input id="aPatient" readonly>
      <label>欠费原因</label>
      <textarea id="aReason" rows="3" placeholder="请填写欠费原因"></textarea>
      <label>追缴进度</label>
      <select id="aProgress">
        <option>未催缴</option><option>协商中</option><option>拒绝缴费</option><option>移交法务发起诉讼</option><option>已缴费</option>
      </select>
      <label style="margin-top:16px">操作历史</label>
      <div id="aHistory" style="max-height:180px;overflow-y:auto;border:1px solid #e5e7eb;border-radius:8px;padding:10px 12px;font-size:12px;line-height:2"></div>
      <div class="actions">
        <button class="btn" onclick="document.getElementById('arrearModal').classList.remove('show')">取消</button>
        <button class="btn primary" onclick="saveArrear()">保存</button>
      </div>
    </div>
  </div>`;
  window._arrearRows = rows;
}
function progressTag(p){
  const map = {"未催缴":"red","协商中":"blue","拒绝缴费":"orange","移交法务发起诉讼":"gray","已缴费":"green"};
  return `<span class="tag ${map[p]||'gray'}">${p}</span>`;
}
function openArrearEdit(no){
  const r = window._arrearRows.find(x=>x.no===no);
  document.getElementById("aPatient").value = `${r.name}（${r.no} · ${r.dept}）`;
  document.getElementById("aReason").value = r.reason||"";
  document.getElementById("aProgress").value = r.progress;
  document.getElementById("aHistory").innerHTML = r.history.length
    ? r.history.map(h=>`<div><b>${h.by}</b> <span style="color:#9ca3af">${h.time}</span><br><span style="color:#475569">${h.action}</span></div>`).join('<hr style="border:none;border-top:1px dashed #e5e7eb;margin:6px 0">')
    : '<span style="color:#cbd5e1">暂无操作历史</span>';
  document.getElementById("arrearModal").classList.add("show");
}
function saveArrear(){
  document.getElementById("arrearModal").classList.remove("show");
  toast("已保存，操作记录已留痕（原型演示）");
}

/* ================= 页面：通报报表 ================= */
function pageReport(){
  const c = renderLayout("report","通报报表（Top榜）");
  const agg = {}, cnt = {};
  ARREARS.forEach(r=>{ agg[r.dept]=(agg[r.dept]||0)+r.arrear; cnt[r.dept]=(cnt[r.dept]||0)+1; });
  const top = Object.entries(agg).sort((a,b)=>b[1]-a[1]);
  const topPatients = [...ARREARS].sort((a,b)=>b.arrear-a.arrear).slice(0,10);
  const medal = ["p1","p2","p3"];
  c.innerHTML = ribbon() + `
  <div class="card">
    <div class="filters">
      <h3 style="margin:0">科室欠费 Top3（截至 2026-08-03 09:00）</h3>
      <div class="spacer"></div>
      <button class="btn" onclick="toast('已导出通报 Excel（原型演示）')">导出通报</button>
      <button class="btn primary" onclick="openPushModal()">生成通报并推送</button>
    </div>
  </div>
  <div class="podium card" style="background:transparent;box-shadow:none;padding:0">
    ${top.slice(0,3).map(([d,a],i)=>`
      <div class="podium-item ${medal[i]}">
        <div class="rank">TOP${i+1}</div>
        <div class="dept">${d}</div>
        <div class="amt">${fmtW(a)} 元</div>
        <div class="cnt">欠费患者 ${cnt[d]} 人 · 科主任与主管医生将收到通报推送</div>
      </div>`).join("")}
  </div>
  <div class="card">
    <h3>全部科室欠费排行</h3>
    ${(()=>{const max=top[0][1];return top.map(([d,a])=>`
      <div class="rank-bar"><div class="dept">${d}</div>
      <div class="bar"><i style="width:${(a/max*100).toFixed(1)}%"></i></div>
      <div class="pct">${fmtW(a)}元</div></div>`).join("");})()}
  </div>
  <div class="card">
    <h3>患者欠费金额 Top10</h3>
    <table class="grid">
      <thead><tr><th>#</th><th>住院号</th><th class="num">住院次数</th><th>姓名</th><th>住院病区</th><th>主管医生</th><th>欠费类型</th><th class="num">欠费金额(元)</th><th>追缴进度</th></tr></thead>
      <tbody>${topPatients.map((r,i)=>`
        <tr><td>${i<3?`<b style="color:#dc2626">${i+1}</b>`:i+1}</td><td>${r.no}</td><td class="num">${r.zy||1}</td><td>${r.name}</td><td>${r.dept}</td><td>${fullDoc(r.doc)}</td>
        <td>${typeTag(r.type)}</td>
        <td class="num money-neg">${fmt(r.arrear)}</td><td>${progressTag(r.progress)}</td></tr>`).join("")}
      </tbody>
    </table>
    <div class="note">通报推送节奏：每周一 09:00 自动推送（一期固定，暂不支持配置）；渠道：企业微信 / 短信（视院内通道而定）；接收人：各欠费科室科主任、主管医生，通报附系统链接引导医生点击查看明细。</div>
  </div>
  <div class="modal-mask" id="pushModal">
    <div class="modal" style="width:520px">
      <h3>通报预览与推送</h3>
      <div class="msg-preview">${buildNotice()}</div>
      <label>推送渠道</label>
      <select><option>企业微信（推荐，院内已有）</option><option>短信</option></select>
      <label>接收人</label>
      <input value="各欠费科室科主任、主管医生" readonly>
      <div class="warn">通报底端附康复医院运营管理系统链接，引导医生点击访问查看明细。推送后将在“推送记录”中留痕（时间、渠道、接收人、操作人），支持失败重发与手动补发。</div>
      <div class="actions">
        <button class="btn" onclick="closePushModal()">取消</button>
        <button class="btn primary" onclick="closePushModal();toast('通报已推送：企业微信 5 人，全部成功（原型演示）')">确认推送</button>
      </div>
    </div>
  </div>`;
}
function openPushModal(){ document.getElementById("pushModal").classList.add("show"); }
function closePushModal(){ document.getElementById("pushModal").classList.remove("show"); }

/* ================= 页面：推送记录 ================= */
function pagePushlog(){
  const c = renderLayout("pushlog","推送记录");
  c.innerHTML = ribbon() + `
  <div class="card">
    <div class="filters">
      <select><option>全部渠道</option><option>企业微信</option></select>
      <select><option>全部状态</option><option>成功</option><option>失败</option></select>
      <input type="date" value="2026-07-01"> <span style="color:#9ca3af">至</span> <input type="date" value="2026-08-03">
      <button class="btn primary" onclick="toast('已按条件筛选（原型演示）')">查询</button>
      <div class="spacer"></div>
      <button class="btn danger" onclick="batchResend()">批量重发</button>
    </div>
  </div>
  <div class="card">
    <h3>推送留痕</h3>
    <table class="grid">
      <thead><tr><th><input type="checkbox" onclick="toggleAllPush(this.checked)"></th><th>推送时间</th><th>渠道</th><th>接收人</th><th>内容</th><th>状态</th><th>触发方式</th><th>操作</th></tr></thead>
      <tbody>${PUSH_LOG.map((l,i)=>`
        <tr><td><input type="checkbox" class="push-cb" value="${i}"></td>
        <td>${l.time}</td><td><span class="tag ${l.channel==="企业微信"?"green":"blue"}">${l.channel}</span></td>
        <td>${l.target}</td><td>${l.content}</td>
        <td>${l.status.startsWith("成功")?'<span class="tag green">成功</span>':`<span class="tag red">${l.status}</span>`}</td>
        <td style="color:#6b7280">${l.by}</td>
        <td><button class="btn small" onclick="toast('已重新推送（原型演示）')">重发</button></td></tr>`).join("")}
      </tbody>
    </table>
    <div class="note">所有通报推送均留痕可追溯：谁收的、什么时候发的、成功还是失败。支持勾选多条记录批量重发（常用于失败后集中补发）。推送渠道统一为企业微信，直接复用院内组织架构。</div>
  </div>`;
}
function toggleAllPush(v){ document.querySelectorAll(".push-cb").forEach(cb=>cb.checked=v); }
function batchResend(){
  const n = document.querySelectorAll(".push-cb:checked").length;
  toast(n ? `已批量重发 ${n} 条（原型演示）` : "请先勾选要重发的记录");
}

/* ================= 页面：预计出院管理（HIS同步） ================= */
function overdueTag(p){
  return p.planDate
    ? `<span class="tag green">已填报</span>`
    : `<span class="tag gray">未填报</span>`;
}
function statusTag(p){
  return p.status==="异常"
    ? `<span class="tag red">异常</span>`
    : `<span class="tag green">正常</span>`;
}
const dash = v => v || '<span style="color:#cbd5e1">—</span>';
function pageDischarge(){
  const c = renderLayout("discharge","预计出院管理");
  const rows = visibleRows(INPATIENTS);
  c.innerHTML = ribbon() + `
  <div class="stat-row" style="grid-template-columns:repeat(5,1fr)">
    <div class="stat-card"><div class="label">在院患者</div><div class="value blue">${rows.length}</div><div class="extra">患者信息每日从 HIS 同步</div></div>
    <div class="stat-card"><div class="label">已填报预计出院时间</div><div class="value green">${rows.filter(p=>p.planDate).length}</div></div>
    <div class="stat-card"><div class="label">预约营养会诊</div><div class="value">${rows.filter(p=>p.nuList.length).length} 人</div><div class="extra">共 ${rows.reduce((s,p)=>s+p.nuList.length,0)} 条预约记录</div></div>
    <div class="stat-card"><div class="label">预约居家康复</div><div class="value">${rows.filter(p=>p.hjList.length).length} 人</div><div class="extra">共 ${rows.reduce((s,p)=>s+p.hjList.length,0)} 条预约记录</div></div>
    <div class="stat-card"><div class="label">预约复诊</div><div class="value">${rows.filter(p=>p.reList.length).length} 人</div><div class="extra">共 ${rows.reduce((s,p)=>s+p.reList.length,0)} 条预约记录</div></div>
  </div>
  <div class="card">
    <div class="filters">
      <select><option>全部科室</option>${DEPTS.map(d=>`<option>${d}</option>`).join("")}</select>
      <select><option>时间类型（全部）</option><option>预约营养会诊时间</option><option>预约居家康复时间</option><option>随访时间</option><option>复诊预约时间</option></select>
      <input type="date" value="2026-07-01"> 至 <input type="date" value="2026-08-31">
      <input placeholder="住院号 / 姓名 / 主管医师" style="width:160px">
      <button class="btn primary" onclick="toast('已按条件筛选（原型演示）')">查询</button>
    </div>
  </div>
  <div class="card">
    <h3>在院患者预计出院情况</h3>
    <div style="overflow-x:auto">
    <table class="grid" style="min-width:1900px">
      <thead><tr><th>患者姓名</th><th>患者性别</th><th>住院号</th><th class="num">住院次数</th><th>所属科室</th><th>入院时间</th><th>主诊断</th><th>主管医师</th><th>预约复诊时间</th><th>预计出院时间</th><th>实际出院时间</th><th>预约营养会诊时间</th><th>预约居家康复时间</th><th>随访时间</th><th>状态</th><th>异常原因</th><th>操作</th></tr></thead>
      <tbody>${rows.map((p,i)=>`
        <tr>
          <td>${p.name}</td><td>${p.gender||'—'}</td><td>${p.no}</td><td class="num">${p.zy||1}</td><td>${p.dept}</td>
          <td>${p.admitDate||'—'}</td><td>${p.diag||'—'}</td><td>${p.doc}</td>
          <td>${dash(latestTime(p.reList))}</td>
          <td>${p.planDate ? p.planDate + (isSoon(p.planDate)?' <span class="tag orange">临近</span>':'') : '<span style="color:#cbd5e1">—</span>'}</td>
          <td>${dash(p.outDate)}</td>
          <td>${dash(latestTime(p.nuList))}</td>
          <td>${dash(latestTime(p.hjList))}</td>
          <td>${dash(latestTime(p.follow))}</td>
          <td>${statusTag(p)}</td>
          <td>${p.exReason?`<span style="color:#dc2626">${p.exReason}</span>`:'<span style="color:#cbd5e1">—</span>'}</td>
          <td><button class="btn small ${p.planDate?'':'primary'}" onclick='openDischargeEdit(${i})'>${p.planDate?'编辑':'填报'}</button></td>
        </tr>`).join("")}
      </tbody>
    </table>
    </div>
    <div class="note">填报规则：在患者预计出院前 2 天进行填报；首次填报保存后不得直接变更，确需变更须填写变更原因，操作历史（操作人、操作时间、变更内容）全程留痕。居家康复 / 营养会诊支持多次添加记录（最多 10 条），列表展示最新一次预约时间；复诊由主管医生预约、门诊部跟踪到诊情况；院后随访按出院第 7 / 30 / 60 天各填报一次。</div>
  </div>
  <div class="modal-mask" id="disModal">
    <div class="modal" style="width:920px;max-height:88vh;overflow-y:auto">
      <h3 id="dTitle">填报预计出院时间</h3>
      <label>患者</label><input id="dPatient" readonly>
      <div style="display:grid;grid-template-columns:1fr 2fr;gap:0 16px;margin-top:4px">
        <div>
          <label>状态</label>
          <select id="dStatus" onchange="document.getElementById('dExWrap').style.display=this.value==='异常'?'block':'none'"><option>正常</option><option>异常</option></select>
        </div>
        <div id="dExWrap" style="display:none">
          <label>异常原因（选择异常时必填，留痕）</label>
          <input id="dExReason" placeholder="如：入院≥7天未填报预计出院时间">
        </div>
      </div>
      <div class="resp-block"><div class="resp-tag">① 主管医生负责填报</div>
        <label>预计出院时间 <span style="color:#dc2626">*（必填）</span></label>
        <input type="date" id="dDate" value="2026-08-10" required style="max-width:280px">
        <div style="display:grid;grid-template-columns:1fr 2fr;gap:0 16px;margin-top:4px">
          <div><label>患者是否为特殊患者</label>
            <select id="dSpecial" onchange="toggleSpecial(this.value)"><option>否</option><option>是</option></select></div>
          <div id="dSpecialWrap" style="display:none">
            <label>特殊原因（选择"是"时必填，留痕）</label>
            <input id="dSpecialReason" placeholder="如：终末期姑息治疗 / 重症无法离院 / 医疗纠纷中">
          </div>
        </div>
        <div id="dSpecialBody">
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:0 16px">
          <div><label>复诊预约时间</label><input type="date" id="dReAppt"></div>
          <div><label>营养会诊预约时间</label><input type="date" id="dNuAppt"></div>
          <div><label>居家康复预约时间</label><input type="date" id="dHjAppt"></div>
        </div>
        <div id="dReasonWrap" style="display:none">
          <label>变更原因（必填，留痕）</label>
          <textarea id="dReason" rows="2" placeholder="首次填报后不得直接变更，变更须填写原因"></textarea>
        </div>
        </div>
      </div>
      <div id="dOtherBlocks">
      <div class="resp-block"><div class="resp-tag" style="color:#7c3aed">门诊部负责填报</div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:0 16px">
          <div><label>预约复诊时间</label><input type="date" id="dVisitAppt"></div>
          <div><label>是否到诊</label>
            <select id="dVisitArrived" onchange="toggleVisit(this.value)"><option value="">未跟踪</option><option>是</option><option>否</option></select></div>
        </div>
        <div id="dVisitYes" style="display:none;display:none;grid-template-columns:1fr 1fr;gap:0 16px">
          <div><label>具体到诊时间</label><input type="date" id="dVisitTime"></div>
          <div><label>填报人</label><input id="dVisitBy" placeholder="门诊部随访人员"></div>
        </div>
        <div id="dVisitNo" style="display:none">
          <label>未到诊原因</label>
          <input id="dVisitReason" placeholder="如：患者迁居外地 / 电话联系不上 / 病情变化已住院">
        </div>
      </div>
      <div class="resp-block"><div class="resp-tag">② 营养科负责填报</div>
        <label style="margin-top:0">营养会诊信息填报（可多条，最多 10 条）</label>
        <div id="dNu"></div>
      </div>
      <div class="resp-block"><div class="resp-tag">③ 居家康复科负责填报</div>
        <label style="margin-top:0">居家康复信息填报（可多条，最多 10 条）</label>
        <div id="dHj"></div>
      </div>
      <div class="resp-block"><div class="resp-tag">④ 护理部负责填报</div>
        <div style="display:grid;grid-template-columns:1fr 2fr;gap:0 16px;margin-bottom:4px">
          <div><label>是否需要回访</label>
            <select id="dNeedFu" onchange="toggleNeedFu(this.value)"><option>是</option><option>否</option></select></div>
          <div id="dNoFuWrap" style="display:none">
            <label>无需回访原因（必填，留痕）</label>
            <input id="dNoFuReason" placeholder="如：患者转往上级医院继续治疗">
          </div>
        </div>
        <div id="dFollow"></div>
      </div>
      </div>
      <label style="margin-top:18px">操作历史</label>
      <div id="dHistory" style="max-height:150px;overflow-y:auto;border:1px solid #e5e7eb;border-radius:8px;padding:10px 12px;font-size:12px;line-height:2"></div>
      <div class="actions">
        <button class="btn" onclick="document.getElementById('disModal').classList.remove('show')">取消</button>
        <button class="btn primary" onclick="saveDischarge()">保存</button>
      </div>
    </div>
  </div>`;
  window._rows = rows;
}
function isSoon(d){ return d <= "2026-08-05"; }
const FOLLOW_DAYS = [7,30,60];
function followBlock(f, day){
  return `<div style="border:1px solid #e5e7eb;border-radius:8px;padding:10px 12px;margin-bottom:10px">
    <div style="font-weight:600;margin-bottom:8px">出院第 ${day} 天回访</div>
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:0 12px">
      <div><label>回访时间</label><input type="date" value="${f.time}"></div>
      <div><label>执行人</label><input value="${f.by}" placeholder="随访护士"></div>
      <div><label>患者恢复情况</label><select>${["","恢复良好","一般","欠佳"].map(v=>`<option ${f.rec===v?'selected':''}>${v||'请选择'}</option>`).join("")}</select></div>
      <div><label>提醒预约复诊</label><select>${["是","否"].map(v=>`<option ${f.remind===v?'selected':''}>${v}</option>`).join("")}</select></div>
      <div><label>患者对居家康复的评价</label><select onchange="this.parentElement.nextElementSibling.style.display=this.value==='不满意'?'block':'none'">${["满意","合格","不满意"].map(v=>`<option ${f.hjEval===v?'selected':''}>${v}</option>`).join("")}</select></div>
      <div style="display:${f.hjEval==='不满意'?'block':'none'}"><label>不满意原因</label><input value="${f.hjReason}" placeholder="选择不满意时必填"></div>
      <div><label>预约居家康复评估</label><input type="date" value="${f.hjAppt}"></div>
      <div><label>对营养的评价</label><select onchange="this.parentElement.nextElementSibling.style.display=this.value==='不满意'?'block':'none'">${["满意","合格","不满意"].map(v=>`<option ${f.nuEval===v?'selected':''}>${v}</option>`).join("")}</select></div>
      <div style="display:${f.nuEval==='不满意'?'block':'none'}"><label>不满意原因</label><input value="${f.nuReason}" placeholder="选择不满意时必填"></div>
      <div><label>预约营养评估</label><input type="date" value="${f.nuAppt}"></div>
    </div>
  </div>`;
}
function svcBlock(list, label, key){
  const rows = (list.length?list:[{time:"",by:"",result:""}]).map((r,idx)=>`
    <div style="border:1px solid #e5e7eb;border-radius:8px;padding:10px 12px;margin-bottom:10px">
      <div style="display:flex;align-items:center;margin-bottom:8px">
        <span style="font-weight:600">${label}${idx+1}</span>
        <div class="spacer"></div>
        ${idx=== (list.length?list.length:0)-1 || (!list.length && idx===0) ? `<button class="btn small" onclick="addSvcRow('${key}','${label}')">＋ 新增一条</button>` : ''}
      </div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:0 12px">
        <div><label>预约时间</label><input type="date" value="${r.time}"></div>
        <div><label>执行人</label><input value="${r.by}"></div>
        <div><label>执行结果</label><input value="${r.result}" placeholder="如：已预约 / 已完成 / 待跟进"></div>
      </div>
    </div>`).join("");
  return `<div id="svc_${key}" data-count="${list.length||1}">${rows}</div>`;
}
function addSvcRow(key, label){
  const box = document.getElementById("svc_"+key);
  const n = +box.dataset.count;
  if(n>=10){ toast("最多添加 10 条记录"); return; }
  box.dataset.count = n+1;
  box.insertAdjacentHTML("beforeend", `
    <div style="border:1px solid #e5e7eb;border-radius:8px;padding:10px 12px;margin-bottom:10px">
      <div style="display:flex;align-items:center;margin-bottom:8px">
        <span style="font-weight:600">${label}${n+1}</span>
        <div class="spacer"></div>
        <button class="btn small" onclick="addSvcRow('${key}','${label}')">＋ 新增一条</button>
      </div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:0 12px">
        <div><label>预约时间</label><input type="date"></div>
        <div><label>执行人</label><input></div>
        <div><label>执行结果</label><input placeholder="如：已预约 / 已完成 / 待跟进"></div>
      </div>
    </div>`);
}
function openDischargeEdit(i){
  const p = window._rows[i];
  const isNew = !p.planDate;
  document.getElementById("dPatient").value = `${p.name}（${p.no} · ${p.dept} · ${p.diag||''}）`;
  document.getElementById("dTitle").textContent = isNew ? "填报预计出院时间" : "编辑院后管理信息";
  document.getElementById("dReasonWrap").style.display = isNew ? "none" : "block";
  if(p.planDate) document.getElementById("dDate").value = p.planDate;
  document.getElementById("dStatus").value = p.status || "正常";
  document.getElementById("dExWrap").style.display = p.status==="异常" ? "block" : "none";
  document.getElementById("dExReason").value = p.exReason || "";
  document.getElementById("dHjAppt").value = latestTime(p.hjList) || "";
  document.getElementById("dNuAppt").value = latestTime(p.nuList) || "";
  document.getElementById("dFollow").innerHTML = followBlock(p.follow[0], FOLLOW_DAYS[0])
    + `<div id="dFuLater">` + followBlock(p.follow[1], FOLLOW_DAYS[1]) + followBlock(p.follow[2], FOLLOW_DAYS[2]) + `</div>`;
  document.getElementById("dReAppt").value = p.reAppt || latestTime(p.reList) || "";
  document.getElementById("dVisitAppt").value = p.reAppt || latestTime(p.reList) || "";
  document.getElementById("dVisitArrived").value = (p.visit && p.visit.arrived) || "";
  document.getElementById("dVisitTime").value = (p.visit && p.visit.time) || "";
  document.getElementById("dVisitBy").value = (p.visit && p.visit.by) || "";
  document.getElementById("dVisitReason").value = (p.visit && p.visit.reason) || "";
  toggleVisit((p.visit && p.visit.arrived) || "");
  document.getElementById("dHj").innerHTML = svcBlock(p.hjList, "居家康复", "hj");
  document.getElementById("dNu").innerHTML = svcBlock(p.nuList, "营养会诊", "nu");
  document.getElementById("dSpecial").value = p.special || "否";
  document.getElementById("dSpecialReason").value = p.specialReason || "";
  toggleSpecial(p.special || "否");
  document.getElementById("dNeedFu").value = p.needFu || "是";
  document.getElementById("dNoFuReason").value = p.noFuReason || "";
  toggleNeedFu(p.needFu || "是");
  const hist = [];
  if(p.planDate) hist.push({time:"首次填报",by:p.doc,action:`填报预计出院时间：${p.changes.length?p.changes[p.changes.length-1].from:p.planDate}`});
  p.changes.forEach(ch=>hist.push({time:ch.time,by:ch.by,action:`变更：${ch.from} → ${ch.to}；原因：${ch.reason}`}));
  if(p.status==="异常") hist.push({time:"2026-08-03 09:10",by:"系统",action:`标记为异常；异常原因：${p.exReason}`});
  latestTime(p.reList) && hist.push({time:"2026-08-01 14:30",by:p.doc,action:`新增复诊预约记录：${latestTime(p.reList)}`});
  latestTime(p.hjList) && hist.push({time:"2026-08-01 14:32",by:p.doc,action:`新增居家康复记录：${latestTime(p.hjList)}`});
  latestTime(p.nuList) && hist.push({time:"2026-08-01 14:35",by:p.doc,action:`新增营养会诊记录：${latestTime(p.nuList)}`});
  document.getElementById("dHistory").innerHTML = hist.length
    ? hist.map(h=>`<div><b>${h.by}</b> <span style="color:#9ca3af">${h.time}</span><br><span style="color:#475569">${h.action}</span></div>`).join('<hr style="border:none;border-top:1px dashed #e5e7eb;margin:6px 0">')
    : '<span style="color:#cbd5e1">暂无操作历史</span>';
  document.getElementById("disModal").classList.add("show");
}
function setGray(id, off){
  const el = document.getElementById(id);
  if(!el) return;
  el.style.opacity = off ? ".45" : "1";
  el.style.pointerEvents = off ? "none" : "auto";
}
function toggleSpecial(v){
  document.getElementById("dSpecialWrap").style.display = v==="是" ? "block" : "none";
  setGray("dSpecialBody", v==="是");
  setGray("dOtherBlocks", v==="是");
}
function toggleVisit(v){
  const yes = document.getElementById("dVisitYes"), no = document.getElementById("dVisitNo");
  yes.style.display = v==="是" ? "grid" : "none";
  no.style.display = v==="否" ? "block" : "none";
}
function toggleNeedFu(v){
  document.getElementById("dNoFuWrap").style.display = v==="否" ? "block" : "none";
  setGray("dFuLater", v==="否");
}
function saveDischarge(){
  document.getElementById("disModal").classList.remove("show");
  toast("已保存，操作记录已留痕（原型演示）");
}
function showChanges(i){ openDischargeEdit(i); }

/* ================= 页面：统计分析 ================= */
function buildTrend(){
  const days = Array.from({length:31},(_,i)=>i+1);
  const w = (d,amp,ph) => Math.sin(d/3+ph)*amp;
  const series = [
    {name:"非计划出院率", color:"#dc2626", data:days.map(d=>+(5.6-0.04*d+w(d,0.8,0)).toFixed(1))},
    {name:"营养会诊预约率", color:"#0d9488", data:days.map(d=>+(54+0.5*d+w(d,3,1)).toFixed(1))},
    {name:"居家康复预约率", color:"#d97706", data:days.map(d=>+(34+0.4*d+w(d,2.5,2)).toFixed(1))},
    {name:"复诊预约率", color:"#2563eb", data:days.map(d=>+(69+0.35*d+w(d,2,3)).toFixed(1))},
  ];
  const W=860,H=220,L=42,B=30,T=16,MAX=100;
  const x = i => L + i*(W-L-16)/(days.length-1);
  const y = v => T + (MAX-v)/MAX*(H-T-B);
  const grid = [0,25,50,75,100].map(v=>`<line x1="${L}" y1="${y(v)}" x2="${W-16}" y2="${y(v)}" stroke="#eef2f7"/><text x="${L-8}" y="${y(v)+4}" font-size="10" fill="#9ca3af" text-anchor="end">${v}%</text>`).join("");
  const xlab = days.filter(d=>d===1||d%5===0).map(d=>`<text x="${x(d-1)}" y="${H-8}" font-size="11" fill="#9ca3af" text-anchor="middle">8/${String(d).padStart(2,"0")}</text>`).join("");
  const lines = series.map(s=>{
    const pts = s.data.map((v,i)=>`${x(i)},${y(v)}`).join(" ");
    return `<polyline points="${pts}" fill="none" stroke="${s.color}" stroke-width="2"/>`;
  }).join("");
  const legend = series.map(s=>`<span style="display:inline-flex;align-items:center;margin-right:18px;font-size:12px;color:#475569"><i style="display:inline-block;width:14px;height:3px;background:${s.color};margin-right:6px;border-radius:2px"></i>${s.name}</span>`).join("");
  return `<div style="margin-bottom:8px">${legend}</div><svg viewBox="0 0 ${W} ${H}" style="width:100%">${grid}${xlab}${lines}</svg>`;
}
function pageBoard(){
  const c = renderLayout("board","统计分析");
  const all = visibleRows(INPATIENTS);
  const rows = all.filter(p=>p.planDate);
  const soon = rows.filter(p=>p.planDate<="2026-08-05").length;
  const week = rows.filter(p=>p.planDate>"2026-08-05"&&p.planDate<="2026-08-10").length;
  const boardRows = dept => {
    const list = dept==="全部科室" ? all : all.filter(p=>p.dept===dept);
    return [...list].sort((a,b)=>(a.planDate||"9999")<(b.planDate||"9999")?-1:1).map(p=>`
      <tr><td>${p.name}</td><td>${p.gender||'—'}</td><td>${p.no}</td><td>${p.dept}</td>
      <td>${p.admitDate||'—'}</td><td>${p.doc}</td>
      <td>${p.planDate ? p.planDate + (isSoon(p.planDate)?' <span class="tag orange">临近</span>':'') : '<span style="color:#cbd5e1">未填报</span>'}</td></tr>`).join("");
  };
  const fuCell = (f, day) => {
    if(!f.time) return `<div style="color:#cbd5e1;font-size:12px">第 ${day} 天 · 未回访</div>`;
    const ev = (v,reason) => v==="不满意"
      ? `<span class="tag red">不满意</span>${reason?`<div style="color:#dc2626;font-size:11px">${reason}</div>`:''}`
      : v==="合格" ? `<span class="tag orange">合格</span>` : `<span class="tag green">满意</span>`;
    return `<div style="font-size:12px;line-height:1.9;min-width:170px">
      <div style="font-weight:600;color:#1e293b;margin-bottom:2px">第 ${day} 天 · ${f.time}</div>
      <div>执行人：${f.by||'—'}</div>
      <div>恢复情况：${f.rec||'—'}</div>
      <div>提醒预约复诊：${f.remind||'—'}</div>
      <div>居家康复评价：${ev(f.hjEval,f.hjReason)}</div>
      <div>预约居家康复评估：${f.hjAppt||'—'}</div>
      <div>营养评价：${ev(f.nuEval,f.nuReason)}</div>
      <div>预约营养评估：${f.nuAppt||'—'}</div>
    </div>`;
  };
  const fuRows = dept => {
    const list = dept==="全部科室" ? visibleRows(DISCHARGED) : visibleRows(DISCHARGED).filter(p=>p.dept===dept);
    return list.map(p=>`
      <tr><td>${p.name}</td><td>${p.gender}</td><td>${p.dept}</td><td>${p.outDate}</td><td>${p.doc}</td>
      <td>${fuCell(p.follow[0],7)}</td>
      <td>${fuCell(p.follow[1],30)}</td>
      <td>${fuCell(p.follow[2],60)}</td></tr>`).join("");
  };
  c.innerHTML = ribbon() + `
  <div class="stat-row" style="grid-template-columns:repeat(3,1fr)">
    <div class="stat-card"><div class="label">3天内预计出院</div><div class="value red">${soon}</div><div class="extra">床位即将释放，医保/运营提前介入</div></div>
    <div class="stat-card"><div class="label">本周内预计出院</div><div class="value blue">${week}</div></div>
    <div class="stat-card"><div class="label">非计划出院率（上月）</div><div class="value">4.6%</div><div class="extra">实际出院时间 ≠ 预计出院时间占比</div></div>
    <div class="stat-card"><div class="label">营养会诊预约率（当月）</div><div class="value" style="color:#0d9488">64.3%</div><div class="extra">当月有营养会诊预约 18 人 / 当月出院 28 人</div></div>
    <div class="stat-card"><div class="label">居家康复预约率（当月）</div><div class="value" style="color:#d97706">42.9%</div><div class="extra">当月有居家康复预约 12 人 / 当月出院 28 人</div></div>
    <div class="stat-card"><div class="label">复诊预约率（当月）</div><div class="value blue">78.6%</div><div class="extra">当月有复诊预约 22 人 / 当月出院 28 人</div></div>
  </div>
  <div class="card">
    <h3>核心指标趋势（当月 · 逐日）</h3>
    ${buildTrend()}
    <div class="note">口径：非计划出院率 = 实际出院时间 ≠ 预计出院时间的患者占比；三项预约率 = 当日累计有对应预约记录的患者数 ÷ 当月出院总人数。患者出院后自动计入当月统计，次月 1 日重新累计。</div>
  </div>
  <div class="card">
    <div style="display:flex;gap:4px;border-bottom:1px solid #e5e7eb;margin-bottom:14px">
      <button id="tab_board" class="tabbtn active" onclick="switchBoardTab('board')">预出院看板</button>
      <button id="tab_fu" class="tabbtn" onclick="switchBoardTab('fu')">院后管理列表</button>
      <button id="tab_nu" class="tabbtn" onclick="switchBoardTab('nu')">营养会诊</button>
      <button id="tab_hj" class="tabbtn" onclick="switchBoardTab('hj')">居家康复</button>
      <button id="tab_re" class="tabbtn" onclick="switchBoardTab('re')">复诊预约</button>
      <button id="tab_ex" class="tabbtn" onclick="switchBoardTab('ex')">异常列表</button>
    </div>
    <div class="filters">
      <select id="boardDept" onchange="applyBoardDept(this.value)">
        <option>全部科室</option>${DEPTS.map(d=>`<option>${d}</option>`).join("")}
      </select>
      <select id="boardTimeType"><option>时间类型（全部）</option><option>入院时间</option><option>预计出院时间</option></select>
      <input type="date" value="2026-07-01"> 至 <input type="date" value="2026-08-31">
      <input id="boardKw" placeholder="患者姓名/住院号/所属科室/主管医师" style="width:200px">
      <button class="btn primary" onclick="toast('已按条件筛选（原型演示）')">查询</button>
      <div class="spacer"></div>
      <button class="btn" onclick="toast('已导出明细（原型演示）')">导出明细</button>
      <button class="btn primary" id="pushOpBtn" onclick="document.getElementById('pushOpModal').classList.add('show')">推送运营</button>
    </div>
    <div id="viewBoard" style="overflow-x:auto;margin-top:12px">
      <table class="grid">
        <thead><tr><th>患者姓名</th><th>患者性别</th><th>住院号</th><th>所属科室</th><th>入院时间</th><th>主管医师</th><th>预计出院时间</th></tr></thead>
        <tbody id="boardBody">${boardRows("全部科室")}</tbody>
      </table>
      <div class="note">查看权限：运营部（全院）；科主任、医生、护士（本科室）。科室筛选仅全院权限角色可用。</div>
    </div>
    <div id="viewFu" style="overflow-x:auto;margin-top:12px;display:none">
      <table class="grid" style="min-width:1200px">
        <thead><tr><th>患者姓名</th><th>患者性别</th><th>所属科室</th><th>出院时间</th><th>主管医师</th><th>第 7 天回访</th><th>第 30 天回访</th><th>第 60 天回访</th></tr></thead>
        <tbody id="fuBody">${fuRows("全部科室")}</tbody>
      </table>
      <div class="note">每个回访节点展示：回访时间、执行人、患者恢复情况、提醒预约复诊、居家康复评价、预约居家康复评估、营养评价、预约营养评估；评价为"不满意"时红色标注并显示原因。查看权限同预出院看板。</div>
    </div>
    <div id="viewNu" style="overflow-x:auto;margin-top:12px;display:none">
      <table class="grid" style="min-width:900px">
        <thead><tr><th>患者姓名</th><th>患者性别</th><th>所属科室</th><th>主管医师</th><th>营养会诊记录</th></tr></thead>
        <tbody id="nuBody">${svcViewRows("nu","全部科室")}</tbody>
      </table>
      <div class="note">展示所有填写了营养会诊记录的患者，会诊记录按时间排列，最多 10 条。查看权限同预出院看板。</div>
    </div>
    <div id="viewHj" style="overflow-x:auto;margin-top:12px;display:none">
      <table class="grid" style="min-width:900px">
        <thead><tr><th>患者姓名</th><th>患者性别</th><th>所属科室</th><th>主管医师</th><th>居家康复记录</th></tr></thead>
        <tbody id="hjBody">${svcViewRows("hj","全部科室")}</tbody>
      </table>
      <div class="note">展示所有填写了居家康复记录的患者，康复记录按时间排列，最多 10 条。查看权限同预出院看板。</div>
    </div>
    <div id="viewRe" style="overflow-x:auto;margin-top:12px;display:none">
      <table class="grid" style="min-width:1000px">
        <thead><tr><th>患者姓名</th><th>患者性别</th><th>住院号</th><th>所属科室</th><th>入院时间</th><th>主管医师</th><th>复诊预约时间</th><th>患者实际到诊时间</th></tr></thead>
        <tbody id="reBody">${reViewRows("全部科室")}</tbody>
      </table>
      <div class="note">复诊预约时间由主管医生填报；实际到诊情况由门诊部跟踪填报（到诊时间 / 未到诊原因），用于跟踪院后复诊转化。查看权限同预出院看板。</div>
    </div>
    <div id="viewEx" style="overflow-x:auto;margin-top:12px;display:none">
      <table class="grid" style="min-width:1200px">
        <thead><tr><th>患者姓名</th><th>患者性别</th><th>住院号</th><th>所属科室</th><th>入院时间</th><th>主诊断</th><th>主管医师</th><th>预计出院时间</th><th>异常原因</th></tr></thead>
        <tbody id="exBody">${exRows("全部科室")}</tbody>
      </table>
      <div class="note">展示在院患者预计出院情况中状态为"异常"的记录，供运营部集中督办；异常原因由主管医生/运营在编辑页维护，修改留痕。</div>
    </div>
  </div>
  <div class="modal-mask" id="pushOpModal">
    <div class="modal" style="width:560px">
      <h3>推送运营部</h3>
      <label>推送内容预览</label>
      <div style="border:1px solid #e5e7eb;border-radius:8px;padding:12px 14px;font-size:13px;line-height:2;background:#f8fafc">
        【预计出院填报提醒】截至 2026-08-03 09:00，全院在院患者 ${all.length} 人，其中未填报预计出院时间 <b style="color:#dc2626">${all.filter(p=>!p.planDate).length} 人</b>。<br>
        未填报数量 Top 医生：<br>
        1. 孟凡*（神经重症康复病房）1 人<br>
        2. 王雪*（神经重症康复病房）1 人<br>
        3. 陈明*（骨与关节病运动康复病房）1 人<br>
        4. 殷秀*（重症康复病房）1 人<br>
        5. 孙丽*（老年医学康复病房）1 人<br>
        请运营部跟进督办，提醒相关科室及时完成填报。
      </div>
      <label style="margin-top:12px">接收人</label><input value="运营部（全体）" readonly>
      <div class="actions">
        <button class="btn" onclick="document.getElementById('pushOpModal').classList.remove('show')">取消</button>
        <button class="btn primary" onclick="document.getElementById('pushOpModal').classList.remove('show');toast('已推送运营部（原型演示）')">确认推送</button>
      </div>
    </div>
  </div>`;
  window._boardRows = boardRows;
  window._fuRows = fuRows;
}
function svcViewRows(type, dept){
  const key = type==="nu" ? "nuList" : "hjList";
  const label = type==="nu" ? "营养会诊" : "居家康复";
  const list = visibleRows(INPATIENTS).filter(p=>p[key].length && (dept==="全部科室"||p.dept===dept));
  if(!list.length) return `<tr><td colspan="5" style="text-align:center;color:#cbd5e1">暂无记录</td></tr>`;
  return list.map(p=>`<tr><td>${p.name}</td><td>${p.gender}</td><td>${p.dept}</td><td>${p.doc}</td>
    <td>${p[key].map((r,i)=>`<div style="font-size:12px;line-height:1.9;border:1px solid #e5e7eb;border-radius:8px;padding:6px 10px;margin-bottom:6px;min-width:240px"><b>${label}${i+1} · ${r.time}</b><br>执行人：${r.by||'—'}　执行结果：${r.result||'—'}</div>`).join("")}</td></tr>`).join("");
}
function exRows(dept){
  const list = visibleRows(INPATIENTS).filter(p=>p.status==="异常" && (dept==="全部科室"||p.dept===dept));
  if(!list.length) return `<tr><td colspan="9" style="text-align:center;color:#cbd5e1">暂无异常记录</td></tr>`;
  return list.map(p=>`<tr><td>${p.name}</td><td>${p.gender}</td><td>${p.no}</td><td>${p.dept}</td><td>${p.admitDate}</td><td>${p.diag}</td><td>${p.doc}</td>
    <td>${p.planDate||'<span style="color:#cbd5e1">未填报</span>'}</td><td><span style="color:#dc2626">${p.exReason}</span></td></tr>`).join("");
}
function reViewRows(dept){
  const list = visibleRows(INPATIENTS).filter(p=>(p.reAppt||p.reList.length) && (dept==="全部科室"||p.dept===dept));
  if(!list.length) return `<tr><td colspan="8" style="text-align:center;color:#cbd5e1">暂无记录</td></tr>`;
  const visitCell = p => {
    if(!p.visit || !p.visit.arrived) return '<span style="color:#cbd5e1">未跟踪</span>';
    if(p.visit.arrived==="是") return `<div style="font-size:12px;line-height:1.9"><b>${p.visit.time}</b><br>填报人：${p.visit.by||'—'}</div>`;
    return `<div style="font-size:12px;line-height:1.9"><span class="tag red">未到诊</span><div style="color:#dc2626">${p.visit.reason||''}</div><div style="color:#9ca3af">填报人：${p.visit.by||'—'}</div></div>`;
  };
  return list.map(p=>`<tr><td>${p.name}</td><td>${p.gender}</td><td>${p.no}</td><td>${p.dept}</td><td>${p.admitDate}</td><td>${p.doc}</td>
    <td>${p.reAppt||latestTime(p.reList)||'—'}</td><td>${visitCell(p)}</td></tr>`).join("");
}
const BOARD_TAB_CONF = {
  board:{view:"viewBoard", types:["入院时间","预计出院时间"], kw:"患者姓名/住院号/所属科室/主管医师", push:true},
  fu:{view:"viewFu", types:["出院时间"], kw:"患者姓名/主管医师"},
  nu:{view:"viewNu", types:["预约营养会诊时间"], kw:"患者姓名/主管医师"},
  hj:{view:"viewHj", types:["预约居家康复时间"], kw:"患者姓名/主管医师"},
  re:{view:"viewRe", types:["复诊预约时间"], kw:"患者姓名/主管医师"},
  ex:{view:"viewEx", types:["入院时间","预计出院时间"], kw:"患者姓名/住院号/所属科室/主管医师"},
};
function switchBoardTab(tab){
  Object.keys(BOARD_TAB_CONF).forEach(k=>{
    document.getElementById(BOARD_TAB_CONF[k].view).style.display = k===tab?"block":"none";
    document.getElementById("tab_"+k).classList.toggle("active", k===tab);
  });
  const cf = BOARD_TAB_CONF[tab];
  document.getElementById("pushOpBtn").style.display = cf.push ? "" : "none";
  document.getElementById("boardTimeType").innerHTML = '<option>时间类型（全部）</option>' + cf.types.map(t=>`<option>${t}</option>`).join("");
  document.getElementById("boardKw").placeholder = cf.kw;
}
function applyBoardDept(dept){
  document.getElementById("boardBody").innerHTML = window._boardRows(dept);
  document.getElementById("fuBody").innerHTML = window._fuRows(dept);
  document.getElementById("nuBody").innerHTML = svcViewRows("nu",dept);
  document.getElementById("hjBody").innerHTML = svcViewRows("hj",dept);
  document.getElementById("reBody").innerHTML = reViewRows(dept);
  document.getElementById("exBody").innerHTML = exRows(dept);
}

/* 路由分发见文件末尾 */

/* ================= 页面：费别系数配置 ================= */
const COEFS = [
 {code:"01",fee:"城镇职工基本医疗保险",coef:0.5,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"02",fee:"城乡居民基本医疗保险",coef:0.7,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"03",fee:"异地医保持卡",coef:0.7,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"04",fee:"异地医保人员",coef:0.7,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"05",fee:"城市老年人医疗保险",coef:0.7,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"06",fee:"征地超转",coef:0.5,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"07",fee:"自费",coef:1.0,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"08",fee:"医保身份待定",coef:1.0,op:"财务科-张会计",time:"2026-07-15 10:20"},
 {code:"09",fee:"公费医疗",coef:1.0,op:"财务科-张会计",time:"2026-07-15 10:20"},
];
function pageConfig(){
  const c = renderLayout("config","费别系数配置");
  c.innerHTML = ribbon() + `
  <div class="card">
    <h3>计算口径</h3>
    <div class="msg-preview" style="background:#eff6ff;border-color:#bfdbfe">应交押金 = 总费用 × 系数（按患者费别取值）
欠押金 = 预交金 − 应交押金
出院患者：医保患者以医保结算后为准，未结算的按费别系数计算</div>
  </div>
  <div class="card">
    <div class="filters">
      <h3 style="margin:0">患者费别系数</h3>
      <div class="spacer"></div>
      <button class="btn" onclick="toast('已新增一行（原型演示）')">新增费别</button>
      <button class="btn primary" onclick="toast('系数已保存，操作已留痕（原型演示）')">保存修改</button>
    </div>
    <table class="grid" style="margin-top:12px">
      <thead><tr><th>费别编码</th><th>费别</th><th style="width:160px">系数</th><th>计算示例（总费用10万元）</th><th>最近修改人</th><th>修改时间</th></tr></thead>
      <tbody>${COEFS.map(k=>`
        <tr><td>${k.code}</td><td>${k.fee}</td>
        <td><input type="number" step="0.05" min="0" max="1" value="${k.coef}" style="width:90px;padding:6px 8px;border:1px solid #d1d5db;border-radius:6px"></td>
        <td style="color:#6b7280">应交押金 = 100,000 × ${k.coef} = <b>${fmt0(100000*k.coef)}</b> 元</td>
        <td>${k.op}</td><td style="color:#9ca3af">${k.time}</td></tr>`).join("")}
      </tbody>
    </table>
    <div class="note">系数调整仅对调整后新导入的数据生效，历史欠费数据不回溯重算；每次修改记录操作人与操作时间。若 HIS 导出模板中直接包含「应交押金」字段，则以模板字段为准，本系数表仅作校验参考。</div>
  </div>`;
}

/* ================= 页面：用户管理 ================= */
const USERS = [
 {acc:"admin",name:"系统管理员",empNo:"XG0001",role:"系统管理员（全院）",dept:"信息科",phone:"138****0001",status:"启用",last:"2026-08-03 08:01"},
 {acc:"zhangkj",name:"张会计",empNo:"CW0012",role:"财务科（全院）",dept:"财务科",phone:"138****0012",status:"启用",last:"2026-08-03 08:12"},
 {acc:"mengfanyu",name:"孟凡宇",empNo:"YS0103",role:"科主任（本科室）",dept:"神经重症康复病房",phone:"139****0103",status:"启用",last:"2026-08-02 17:40"},
 {acc:"wangxuemei",name:"王雪梅",empNo:"YS0105",role:"科主任（本科室）",dept:"神经重症康复病房",phone:"139****0105",status:"启用",last:"2026-08-01 09:22"},
 {acc:"liuhushi",name:"刘护士",empNo:"HS0201",role:"护士（本科室）",dept:"神经重症康复病房",phone:"137****0201",status:"启用",last:"2026-08-03 07:55"},
 {acc:"liyunying",name:"李运营",empNo:"YY0031",role:"运营部（全院）",dept:"运营部",phone:"136****0031",status:"启用",last:"2026-08-02 14:30"},
 {acc:"wangyibao",name:"王医保",empNo:"YB0022",role:"医保办（全院）",dept:"医保办",phone:"136****0022",status:"启用",last:"2026-07-31 16:08"},
 {acc:"zhaofawu",name:"赵法务",empNo:"FW0008",role:"法务（全院）",dept:"法务部",phone:"135****0008",status:"启用",last:"2026-07-28 11:45"},
 {acc:"raozhiqiang",name:"饶志强",empNo:"YS0207",role:"科主任（本科室）",dept:"重症康复病房",phone:"139****0207",status:"停用",last:"2026-06-19 10:02"},
];
function pageUsers(){
  const c = renderLayout("users","用户管理");
  c.innerHTML = ribbon() + `
  <div class="card">
    <div class="filters">
      <input placeholder="账号 / 姓名" style="width:180px">
      <select><option>全部角色</option>${ROLES_ALL.map(r=>`<option>${r}</option>`).join("")}</select>
      <select><option>全部状态</option><option>启用</option><option>停用</option></select>
      <button class="btn primary" onclick="toast('已按条件筛选（原型演示）')">查询</button>
      <div class="spacer"></div>
      <button class="btn primary" onclick="openUserModal()">新增用户</button>
    </div>
  </div>
  <div class="card">
    <h3>用户列表</h3>
    <table class="grid">
      <thead><tr><th>用户名</th><th>姓名</th><th>工号</th><th>角色</th><th>所属科室</th><th>手机号</th><th>状态</th><th>最近登录</th><th>操作</th></tr></thead>
      <tbody>${USERS.map(u=>`
        <tr><td>${u.acc}</td><td>${u.name}</td><td>${u.empNo}</td><td>${u.role}</td><td>${u.dept}</td><td>${u.phone}</td>
        <td>${u.status==="启用"?'<span class="tag green">启用</span>':'<span class="tag gray">停用</span>'}</td>
        <td style="color:#9ca3af">${u.last}</td>
        <td>
          <button class="btn small" onclick="openUserModal()">编辑</button>
          <button class="btn small ${u.status==='启用'?'danger':''}" onclick="toast('${u.status==="启用"?"已停用":"已启用"}（原型演示）')">${u.status==="启用"?"停用":"启用"}</button>
        </td></tr>`).join("")}
      </tbody>
    </table>
    <div class="note">正式版建议对接院内统一认证，本页面仅维护「谁能登录、是什么角色」；所有增删改操作留痕。</div>
  </div>
  <div class="modal-mask" id="userModal">
    <div class="modal">
      <h3>新增用户</h3>
      <label>姓名</label><input id="uName" placeholder="输入姓名后自动生成用户名" oninput="genAcc(this.value)">
      <label>用户名（自动生成）</label><input id="uAcc" readonly placeholder="姓名拼音，重名顺延加数字，如 wangwu、wangwu1">
      <label>工号</label><input placeholder="如 YS0108">
      <label>角色</label><select>${ROLES_ALL.map(r=>`<option>${r}</option>`).join("")}</select>
      <label>所属科室</label><select><option>财务科</option>${DEPTS.map(d=>`<option>${d}</option>`).join("")}</select>
      <label>手机号</label><input placeholder="用于接收企微/短信通知" maxlength="11">
      <label>状态</label><select><option>启用</option><option>停用</option></select>
      <div class="note" style="margin-top:4px">用户名规则：姓名的全拼小写；与他人重名时，从 1 开始顺延加数字（如 wangwu1、wangwu2），生成后不可修改。</div>
      <div class="actions">
        <button class="btn" onclick="document.getElementById('userModal').classList.remove('show')">取消</button>
        <button class="btn primary" onclick="document.getElementById('userModal').classList.remove('show');toast('已保存（原型演示）')">保存</button>
      </div>
    </div>
  </div>`;
}
/* 原型演示：仅演示"王五"重名自动加数字的生成效果 */
function genAcc(name){
  const box = document.getElementById("uAcc");
  if(!name){ box.value=""; return; }
  box.value = name.includes("王五") ? "wangwu1（与现有 wangwu 重名，自动顺延）" : "（保存时由系统按姓名拼音生成）";
}
function openUserModal(){ document.getElementById("userModal").classList.add("show"); }

/* ================= 页面：权限管理 ================= */
const ROLE_MATRIX = [
 {role:"系统管理员（全院）",arrears:true,discharge:true,sys:true,scope:"全院",actions:"所有权限"},
 {role:"财务科（全院）",arrears:true,discharge:false,sys:false,scope:"全院",actions:"编辑催缴信息 / 导入 / 导出 / 触发推送"},
 {role:"法务（全院）",arrears:true,discharge:false,sys:false,scope:"全院",actions:"查看 / 编辑追缴进度（诉讼相关）"},
 {role:"运营部（全院）",arrears:false,discharge:true,sys:false,scope:"全院",actions:"查看 / 导出 / 触发推送"},
 {role:"医保办（全院）",arrears:false,discharge:true,sys:false,scope:"全院",actions:"查看 / 导出"},
 {role:"科主任（本科室）",arrears:true,discharge:true,sys:false,scope:"本科室",actions:"查看 / 编辑催缴信息 / 填报预计出院"},
 {role:"护士（本科室）",arrears:true,discharge:true,sys:false,scope:"本科室",actions:"查看 / 填报预计出院"},
];
function pageRoles(){
  const c = renderLayout("roles","权限管理");
  c.innerHTML = ribbon() + `
  <div class="card">
    <div class="filters">
      <h3 style="margin:0">角色权限矩阵</h3>
      <div class="spacer"></div>
      <button class="btn" onclick="toast('已新增角色（原型演示）')">新增角色</button>
    </div>
    <table class="grid" style="margin-top:12px">
      <thead><tr><th>角色</th><th>欠费管理</th><th>预出院管理</th><th>系统管理</th><th>数据范围</th><th>允许操作</th><th>操作</th></tr></thead>
      <tbody>${ROLE_MATRIX.map(r=>`
        <tr><td><b>${r.role}</b></td>
        <td>${r.arrears?'<span class="tag green">✓</span>':'<span style="color:#cbd5e1">—</span>'}</td>
        <td>${r.discharge?'<span class="tag green">✓</span>':'<span style="color:#cbd5e1">—</span>'}</td>
        <td>${r.sys?'<span class="tag green">✓</span>':'<span style="color:#cbd5e1">—</span>'}</td>
        <td><span class="tag ${r.scope==="全院"?"blue":"orange"}">${r.scope}</span></td>
        <td style="color:#475569">${r.actions}</td>
        <td><button class="btn small" onclick="openRoleModal('${r.role}')">配置</button></td></tr>`).join("")}
      </tbody>
    </table>
    <div class="note">权限控制到「模块 + 数据范围 + 操作」三层；角色变更后即时生效；所有配置修改留痕（操作人、时间、变更内容）。科主任/护士的数据范围固定为本科室，不可配置为全院。</div>
  </div>
  <div class="modal-mask" id="roleModal">
    <div class="modal">
      <h3>配置角色权限</h3>
      <label>角色</label><input id="rName" readonly>
      <label>可见模块</label>
      <div style="line-height:2.2;font-size:13px">
        <label style="display:inline-block;margin:0 14px 0 0"><input type="checkbox" checked> 欠费管理</label>
        <label style="display:inline-block;margin:0 14px 0 0"><input type="checkbox"> 预出院管理</label>
        <label style="display:inline-block;margin:0"><input type="checkbox"> 系统管理</label>
      </div>
      <label>数据范围</label>
      <select><option>全院</option><option>本科室</option></select>
      <label>允许操作</label>
      <div style="line-height:2.2;font-size:13px">
        <label style="display:inline-block;margin:0 14px 0 0"><input type="checkbox" checked> 查看</label>
        <label style="display:inline-block;margin:0 14px 0 0"><input type="checkbox" checked> 编辑</label>
        <label style="display:inline-block;margin:0 14px 0 0"><input type="checkbox" checked> 导出</label>
        <label style="display:inline-block;margin:0"><input type="checkbox"> 触发推送</label>
      </div>
      <div class="actions">
        <button class="btn" onclick="document.getElementById('roleModal').classList.remove('show')">取消</button>
        <button class="btn primary" onclick="document.getElementById('roleModal').classList.remove('show');toast('权限已保存并即时生效（原型演示）')">保存</button>
      </div>
    </div>
  </div>`;
}
function openRoleModal(name){
  document.getElementById("rName").value = name;
  document.getElementById("roleModal").classList.add("show");
}

/* ================= 路由分发（必须在所有常量定义之后） ================= */
const PAGE = document.body.dataset.page;
if(PAGE==="arrears") pageArrears();
if(PAGE==="report") pageReport();
if(PAGE==="pushlog") pagePushlog();
if(PAGE==="discharge") pageDischarge();
if(PAGE==="board") pageBoard();
if(PAGE==="config") pageConfig();
if(PAGE==="users") pageUsers();
if(PAGE==="roles") pageRoles();
