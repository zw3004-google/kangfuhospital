export const shouldRedirectToPasswordChange = (mustChangePassword: boolean, currentPath: string) =>
  mustChangePassword && currentPath !== '/login' && currentPath !== '/system/change-password'
