$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host 'Running Stage 3 business workflow verification...'
& (Join-Path $PSScriptRoot 'verify-stage2.ps1')
if ($LASTEXITCODE -ne 0) { throw "Stage 3 verification failed with exit code $LASTEXITCODE" }

$reports = Get-ChildItem (Join-Path $projectRoot 'server\target\surefire-reports\TEST-*.xml')
$totals = @{ tests = 0; failures = 0; errors = 0; skipped = 0 }
foreach ($report in $reports) {
    [xml]$result = Get-Content -Raw $report.FullName
    foreach ($name in @('tests','failures','errors','skipped')) { $totals[$name] += [int]$result.testsuite.$name }
}
if ($totals.failures -ne 0 -or $totals.errors -ne 0 -or $totals.skipped -ne 0) {
    throw 'Stage 3 backend tests did not pass completely'
}
if ($totals.tests -lt 23) { throw "Stage 3 expected at least 23 backend tests, found $($totals.tests)" }

$integrationReport = Join-Path $projectRoot 'server\target\surefire-reports\TEST-cn.hospital.rehab.integration.ApplicationPostgresIntegrationTest.xml'
[xml]$integration = Get-Content -Raw $integrationReport
if ([int]$integration.testsuite.tests -lt 12) { throw 'Stage 3 business integration coverage is incomplete' }

$migration = Join-Path $projectRoot 'server\src\main\resources\db\migration\V13__phase3_push_attempts.sql'
if (-not (Test-Path $migration)) { throw 'Stage 3 Flyway V13 migration is missing' }
Write-Host "Stage 3 verification completed: $($totals.tests) backend tests, 0 skipped."
