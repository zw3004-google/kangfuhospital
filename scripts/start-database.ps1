$projectRoot = Split-Path -Parent $PSScriptRoot
docker compose -f (Join-Path $projectRoot 'deploy\compose.local.yml') up -d
