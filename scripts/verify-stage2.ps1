$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host 'Running Stage 2 permission and security verification...'
& (Join-Path $PSScriptRoot 'verify-stage1.ps1')
if ($LASTEXITCODE -ne 0) { throw "Stage 2 verification failed with exit code $LASTEXITCODE" }

$phase2Report = Join-Path $projectRoot 'server\target\surefire-reports\TEST-cn.hospital.rehab.integration.ApplicationPostgresIntegrationTest.xml'
if (-not (Test-Path $phase2Report)) { throw 'Stage 2 integration test report was not generated' }
[xml]$result = Get-Content -Raw $phase2Report
if ([int]$result.testsuite.failures -ne 0 -or [int]$result.testsuite.errors -ne 0 -or [int]$result.testsuite.skipped -ne 0) {
    throw 'Stage 2 permission integration tests did not pass completely'
}
Write-Host 'Stage 2 verification completed.'
