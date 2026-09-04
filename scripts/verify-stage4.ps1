$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Assert-Contains([string]$path, [string]$pattern, [string]$message) {
    if (-not (Select-String -LiteralPath $path -Pattern $pattern -SimpleMatch -Quiet)) { throw $message }
}

Write-Host 'Running Stage 4 frontend integration verification...'
& (Join-Path $PSScriptRoot 'verify-stage3.ps1')
if ($LASTEXITCODE -ne 0) { throw "Stage 4 verification failed with exit code $LASTEXITCODE" }

$web = Join-Path $projectRoot 'web'
Assert-Contains (Join-Path $web 'src\router\index.ts') 'import(`../views/${name}.vue`)' 'Route lazy loading is missing'
Assert-Contains (Join-Path $web 'src\views\ImportBatchView.vue') '/import-batches/' 'Import error detail integration is missing'
Assert-Contains (Join-Path $web 'src\views\ArrearsPushRecordsView.vue') '/attempts' 'Push attempt integration is missing'
Assert-Contains (Join-Path $web 'src\views\AuditLogView.vue') '/system/audit-logs' 'Audit log integration is missing'
Assert-Contains (Join-Path $web 'src\views\ArrearsDetailsView.vue') 'el-pagination' 'Arrears pagination is missing'
Assert-Contains (Join-Path $web 'src\views\DischargeAnalysisView.vue') 'Cumulative' 'Discharge cumulative trend verification marker is missing'

$entry = Get-ChildItem (Join-Path $web 'dist\assets\index-*.js') | Sort-Object Length | Select-Object -First 1
if (-not $entry -or $entry.Length -gt 100KB) { throw 'Frontend entry chunk exceeds the Stage 4 limit' }
Write-Host "Stage 4 verification completed: entry chunk $([math]::Round($entry.Length / 1KB, 2)) KB."
