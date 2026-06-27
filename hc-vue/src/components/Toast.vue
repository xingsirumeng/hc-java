<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <Teleport to="body">
    <div class="toast-container">
      <transition-group name="toast" tag="div" class="toast-list">
        <div
          v-for="t in toasts"
          :key="t.id"
          class="toast-item"
          :class="'toast-' + t.type"
        >
          <span class="toast-icon">
            {{ t.type === 'success' ? '✅' : t.type === 'error' ? '❌' : 'ℹ️' }}
          </span>
          <span class="toast-text">{{ t.text }}</span>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  pointer-events: none;
}

.toast-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  white-space: nowrap;
}

.toast-success {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}

.toast-error {
  background: #fff2f0;
  color: #cf1322;
  border: 1px solid #ffccc7;
}

.toast-info {
  background: #e6f7ff;
  color: #096dd9;
  border: 1px solid #91d5ff;
}

.toast-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.toast-text {
  line-height: 1.4;
}

/* 淡入淡出动画 */
.toast-enter-active {
  transition: all 0.35s ease-out;
}

.toast-leave-active {
  transition: all 0.35s ease-in;
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
