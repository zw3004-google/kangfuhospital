param(
  [string]$Version = "1.1.0-test-$(Get-Date -Format yyyyMMdd)-x86_64-full",
  [string]$DependencyRoot = ""
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$artifactVersion = ($Version -replace '^V', '' -replace '-test-.*$', '')
$webPackage = Get-Content -LiteralPath (Join-Path $root 'web\package.json') -Raw | ConvertFrom-Json
[xml]$serverPom = Get-Content -LiteralPath (Join-Path $root 'server\pom.xml')
if ($webPackage.version -ne $artifactVersion) {
  throw "Frontend package.json version $($webPackage.version) does not match package version $artifactVersion. Update the version and rebuild the frontend."
}
if ($serverPom.project.version -ne $artifactVersion) {
  throw "Backend pom.xml version $($serverPom.project.version) does not match package version $artifactVersion. Update the version and rebuild the backend."
}
$webDist = Join-Path $root 'web\dist'
if (!(Test-Path -LiteralPath (Join-Path $webDist 'index.html'))) {
  throw 'Frontend build output is missing. Run pnpm build or npm run build first.'
}
if (!(Get-ChildItem -LiteralPath $webDist -Recurse -File | Select-String -SimpleMatch -Quiet $artifactVersion)) {
  throw "Frontend output does not embed version $artifactVersion. Rebuild with pnpm build or npm run build; do not run vite build directly."
}
$staging = Join-Path $root "outputs\kangfu-$Version"
$archive = Join-Path $root "outputs\kangfu-$Version.tar.gz"
if (Test-Path $staging) { Remove-Item -LiteralPath $staging -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$staging\app\web", "$staging\runtime", "$staging\rpms", "$staging\checksums", "$staging\docs" | Out-Null
Copy-Item "$root\deploy\test-package\*" $staging -Recurse -Force
Copy-Item "$root\docs\RELEASE_NOTES.md" "$staging\docs\RELEASE_NOTES.md" -Force
$releaseDeploymentGuide = Join-Path $root "docs\DEPLOYMENT_V$artifactVersion.md"
if (Test-Path $releaseDeploymentGuide) { Copy-Item $releaseDeploymentGuide "$staging\docs\DEPLOYMENT_V$artifactVersion.md" -Force }
if ($DependencyRoot) {
  $resolvedDeps = (Resolve-Path $DependencyRoot).Path
  if (Test-Path "$resolvedDeps\rpms") { Copy-Item "$resolvedDeps\rpms\*.rpm" "$staging\rpms" -Force }
  if (Test-Path "$resolvedDeps\runtime\jre-21") { Copy-Item "$resolvedDeps\runtime\jre-21" "$staging\runtime" -Recurse -Force }
}
Copy-Item "$root\server\target\kangfu-server-$artifactVersion.jar" "$staging\app\kangfu-server.jar"
Copy-Item "$root\web\dist\*" "$staging\app\web" -Recurse -Force
Get-ChildItem "$staging\scripts\*.sh" | ForEach-Object { $text = [IO.File]::ReadAllText($_.FullName); [IO.File]::WriteAllText($_.FullName, ($text -replace "`r`n", "`n"), [Text.UTF8Encoding]::new($false)) }
$manifest = Get-ChildItem $staging -Recurse -File | Where-Object { $_.FullName -notlike '*\checksums\SHA256SUMS' } | ForEach-Object {
  $relative = $_.FullName.Substring($staging.Length + 1).Replace('\','/')
  "{0}  {1}" -f (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower(), $relative
}
[IO.File]::WriteAllText("$staging\checksums\SHA256SUMS", (($manifest -join "`n") + "`n"), [Text.UTF8Encoding]::new($false))
if (Test-Path $archive) { Remove-Item -LiteralPath $archive -Force }
tar -czf $archive -C (Split-Path $staging -Parent) (Split-Path $staging -Leaf)
$archiveHash = (Get-FileHash $archive -Algorithm SHA256).Hash.ToLower()
[IO.File]::WriteAllText("$archive.sha256", "$archiveHash  $(Split-Path $archive -Leaf)`n", [Text.UTF8Encoding]::new($false))
Write-Output $archive
