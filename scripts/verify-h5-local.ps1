param([switch]$SkipBackendIntegration)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
function Assert-LastExitCode([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed with exit code $LASTEXITCODE" } }

Push-Location (Join-Path $projectRoot 'web')
try {
  npm run typecheck
  Assert-LastExitCode 'frontend typecheck'
  npm run test:unit
  Assert-LastExitCode 'frontend tests'
  npm run build
  Assert-LastExitCode 'frontend build'
} finally { Pop-Location }

if (-not $SkipBackendIntegration) {
  . (Join-Path $PSScriptRoot 'dev-env.ps1')
  Push-Location (Join-Path $projectRoot 'server')
  try { mvn -q test; Assert-LastExitCode 'backend tests' } finally { Pop-Location }
}

$required = @(
  'web/src/sessionDraft.ts',
  'web/src/api/http.test.ts',
  'server/src/main/java/cn/hospital/rehab/common/api/ConcurrentUpdateException.java',
  'server/src/main/java/cn/hospital/rehab/common/config/SessionSecurityFilter.java',
  'server/src/main/java/cn/hospital/rehab/common/web/CsrfController.java',
  'scripts/verify-h5-browser.cjs',
  'scripts/verify-h5-session-load.cjs',
  'docs/h5/H5-UAT与上线检查清单.md'
)
foreach ($path in $required) { if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $path))) { throw "missing H5 artifact: $path" } }
Write-Output 'H5 local verification passed. No deployment was performed.'
