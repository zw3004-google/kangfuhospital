<script setup lang="ts">
const modules = [
  { title: '欠费管理', description: '患者欠费导入、追缴填报、通报报表及企微推送', path: '/arrears/details', color: '#2f6fed' },
  { title: '预出院管理', description: '预计出院填报、预约随访、异常识别及统计分析', path: '/discharge/management', color: '#11a683' },
  { title: '系统管理', description: '科室、用户、角色、数据权限及费别系数配置', path: '/system/users', color: '#7857d8' },
]
</script>

<template>
  <section>
    <div class="welcome-card">
      <div>
        <p>本地开发环境</p>
        <h2>康复医院运营管理系统</h2>
        <span>第一期工程已启动，当前正在建设基础数据与导入能力。</span>
      </div>
      <div class="status-badge">开发中</div>
    </div>
    <div class="module-grid">
      <router-link v-for="module in modules" :key="module.title" :to="module.path" class="module-card">
        <span class="module-accent" :style="{ background: module.color }"></span>
        <h3>{{ module.title }}</h3>
        <p>{{ module.description }}</p>
        <span class="module-link">进入模块 →</span>
      </router-link>
    </div>
  </section>
</template>
