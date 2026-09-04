$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Assert-Contains([string]$path, [string]$pattern, [string]$message) {
    if (-not (Select-String -LiteralPath $path -Pattern $pattern -SimpleMatch -Quiet)) { throw $message }
}

Write-Host 'Running Stage 6 system-test baseline verification...'
& (Join-Path $PSScriptRoot 'verify-stage4.ps1')
if ($LASTEXITCODE -ne 0) { throw "Stage 6 verification failed with exit code $LASTEXITCODE" }

$reports = Get-ChildItem (Join-Path $projectRoot 'server\target\surefire-reports\TEST-*.xml')
$totals = @{ tests = 0; failures = 0; errors = 0; skipped = 0 }
foreach ($report in $reports) {
    [xml]$result = Get-Content -Raw $report.FullName
    foreach ($name in @('tests','failures','errors','skipped')) { $totals[$name] += [int]$result.testsuite.$name }
}
if ($totals.failures -ne 0 -or $totals.errors -ne 0 -or $totals.skipped -ne 0) {
    throw 'Stage 6 backend baseline did not pass completely'
}
if ($totals.tests -lt 31) { throw "Stage 6 expected at least 31 backend tests, found $($totals.tests)" }

$integrationReport = Join-Path $projectRoot 'server\target\surefire-reports\TEST-cn.hospital.rehab.integration.ApplicationPostgresIntegrationTest.xml'
[xml]$integration = Get-Content -Raw $integrationReport
if ([int]$integration.testsuite.tests -lt 12) { throw 'Stage 6 PostgreSQL integration coverage is incomplete' }

Assert-Contains (Join-Path $projectRoot 'server\src\main\java\cn\hospital\rehab\arrears\importer\ArrearsImportService.java') 'rows.size() > 1000' 'Arrears 1000-row import limit is missing'
Assert-Contains (Join-Path $projectRoot 'server\src\main\java\cn\hospital\rehab\discharge\importer\DischargeImportService.java') 'rows.size() > 1000' 'Discharge 1000-row import limit is missing'

$phase6Document = Join-Path $projectRoot 'docs\phase6\README.md'
if (-not (Test-Path -LiteralPath $phase6Document)) { throw 'Stage 6 execution record is missing' }
Assert-Contains $phase6Document 'S6-01' 'Stage 6 execution checklist is incomplete'
Assert-Contains $phase6Document 'S6-G1' 'Stage 5 transition gate is missing'

Write-Host "Stage 6 baseline completed: $($totals.tests) backend tests, 0 skipped; execution checklist is ready."
