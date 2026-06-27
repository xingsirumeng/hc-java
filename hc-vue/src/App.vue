<script setup lang="ts">
import Toast from '@/components/Toast.vue'
</script>

<template>
  <router-view />
  <Toast />
</template>

<style>
* {
  margin: 0;
  padding: 0;
}

body {
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
