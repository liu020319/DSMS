import { onBeforeUnmount, onMounted, ref } from 'vue'

export function useMobile(breakpoint = 760) {
  const isMobile = ref(window.innerWidth <= breakpoint)
  const sync = () => { isMobile.value = window.innerWidth <= breakpoint }
  onMounted(() => window.addEventListener('resize', sync, { passive: true }))
  onBeforeUnmount(() => window.removeEventListener('resize', sync))
  return isMobile
}
