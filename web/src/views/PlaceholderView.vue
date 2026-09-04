<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '功能模块'))
</script>

<template>
  <div class="placeholder-card">
    <div class="placeholder-icon">建</div>
    <h2>{{ title }}</h2>
    <p>模块基础路由已建立，后续按照需求基线逐步实现。</p>
  </div>
</template>
