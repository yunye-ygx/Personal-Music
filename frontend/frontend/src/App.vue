<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <NavBar />

    <!-- 主内容区 -->
    <main class="main-content">
      <RouterView />
    </main>

    <!-- 底部播放器 -->
    <AudioPlayer />
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import NavBar from './components/NavBar.vue'
import AudioPlayer from './components/AudioPlayer.vue'
</script>

<style scoped>
.app-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 40px;
  padding-bottom: 110px; /* 给播放器留空间 */
}
</style>
