import type { Directive } from 'vue'
import { hasPermission, loadPermissions } from '../auth'
export const permission: Directive<HTMLElement,string> = { mounted(el,binding){const check=()=>{if(!hasPermission(binding.value)) el.remove()};loadPermissions().then(check); if(!hasPermission(binding.value)) el.style.display='none'} }
