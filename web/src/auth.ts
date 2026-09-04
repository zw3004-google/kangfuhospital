import { http, type ApiResponse } from './api/http'
let permissions = new Set<string>(); let loaded=false
export async function loadPermissions(){if(loaded)return permissions;try{const r=await http.get<ApiResponse<{authorities:string[]}>>('/system/me');permissions=new Set(r.data.data.authorities);loaded=true}catch{permissions=new Set();loaded=true}return permissions}
export function hasPermission(code:string){return permissions.has(code)||permissions.has('ROLE_SYSTEM_ADMIN')}
export function clearPermissions(){permissions=new Set();loaded=false}
