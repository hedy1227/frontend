<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">
        <span>劲酒Geo-BI</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <el-sub-menu index="market">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>前端市场洞察</span>
          </template>
          <el-menu-item index="/market/heatmap">前端分布热力图</el-menu-item>
          <el-menu-item index="/market/insight">空白网点挖掘</el-menu-item>
          <el-menu-item index="/market/flow">订单流向</el-menu-item>
          <el-menu-item index="/market/core-atmosphere">核心氛围街区</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="user">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户洞察与推广</span>
          </template>
          <el-menu-item index="/user/select">产品推广选区</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="manage">
          <template #title>
            <el-icon><DataLine /></el-icon>
            <span>管理赋能</span>
          </template>
          <el-menu-item index="/manage/weak">薄弱区域洞察</el-menu-item>
          <el-menu-item index="/manage/evaluate">人员效能评估</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">admin</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

const handleCommand = (command) => {
  if (command === 'logout') {
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.el-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.el-main {
  background-color: #f5f7fa;
}
</style>
