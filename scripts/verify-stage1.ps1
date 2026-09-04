$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Assert-NativeSuccess([string]$operation) {
    if ($LASTEXITCODE -ne 0) { throw "$operation failed with exit code $LASTEXITCODE" }
}

. (Join-Path $PSScriptRoot 'dev-env.ps1')

Write-Host 'Checking Docker Engine...'
docker version | Out-Host
Assert-NativeSuccess 'docker version'
docker info | Out-Host
Assert-NativeSuccess 'docker info'

# Testcontainers/docker-java does not read the Docker CLI context automatically.
# Docker Engine 29 also requires API 1.44 or newer.
$dockerContext = (docker context show).Trim()
Assert-NativeSuccess 'docker context show'
$dockerContextDetails = docker context inspect $dockerContext | ConvertFrom-Json
Assert-NativeSuccess 'docker context inspect'
$env:DOCKER_HOST = $dockerContextDetails[0].Endpoints.docker.Host

Write-Host 'Starting local PostgreSQL...'
docker compose -f (Join-Path $projectRoot 'deploy\compose.local.yml') up -d --wait
Assert-NativeSuccess 'docker compose up'

Write-Host 'Running backend unit and PostgreSQL integration tests...'
Push-Location (Join-Path $projectRoot 'server')
try {
    mvn '-Dapi.version=1.44' test
    Assert-NativeSuccess 'mvn test'
    $reports = Get-ChildItem 'target\surefire-reports\TEST-*.xml'
    $skipped = ($reports | ForEach-Object { ([xml](Get-Content -Raw $_.FullName)).testsuite.skipped } |
        Measure-Object -Sum).Sum
    if ($skipped -ne 0) { throw "Backend verification skipped $skipped test(s)" }
} finally { Pop-Location }

Write-Host 'Building frontend...'
Push-Location (Join-Path $projectRoot 'web')
try { pnpm.cmd build; Assert-NativeSuccess 'pnpm build' } finally { Pop-Location }

Write-Host 'Stage 1 verification completed.'
