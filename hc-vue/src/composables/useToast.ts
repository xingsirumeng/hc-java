import { ref } from 'vue'

export interface ToastMessage {
  id: number
  text: string
  type: 'success' | 'error' | 'info'
}

const toasts = ref<ToastMessage[]>([])
let nextId = 0

export function useToast() {
  function showToast(text: string, type: 'success' | 'error' | 'info' = 'info') {
    const id = nextId++
    toasts.value.push({ id, text, type })
    setTimeout(() => {
      toasts.value = toasts.value.filter(t => t.id !== id)
    }, 2500)
  }

  function showSuccess(text: string) { showToast(text, 'success') }
  function showError(text: string) { showToast(text, 'error') }
  function showInfo(text: string) { showToast(text, 'info') }

  return { toasts, showToast, showSuccess, showError, showInfo }
}
