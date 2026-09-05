param(
  [string]$Version = "1.1.0-test-$(Get-Date -Format yyyyMMdd)-x86_64-full",
  [string]$DependencyRoot = ""
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$artifactVersion = ($Version -replace '^V', '' -replace '-test-.*$', '')
$staging = Join-Path $root "outputs\kangfu-$Version"
$archive = Join-Path $root "outputs\kangfu-$Version.tar.gz"
if (Test-Path $staging) { Remove-Item -LiteralPath $staging -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$staging\app\web", "$staging\runtime", "$staging\rpms", "$staging\checksums" | Out-Null
Copy-Item "$root\deploy\test-package\*" $staging -Recurse -Force
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
Write-Output $archive
