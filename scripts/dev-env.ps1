$projectRoot = Split-Path -Parent $PSScriptRoot
$javaDirectory = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\java') -Directory | Select-Object -First 1
$mavenDirectory = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\maven') -Directory | Select-Object -First 1

if (-not $javaDirectory -or -not $mavenDirectory) {
    throw 'Local Java or Maven is not installed.'
}

$env:JAVA_HOME = $javaDirectory.FullName
$env:MAVEN_HOME = $mavenDirectory.FullName
$mavenRepository = Join-Path $projectRoot '.tools\m2-repository'
New-Item -ItemType Directory -Force -Path $mavenRepository | Out-Null
$env:MAVEN_OPTS = "-Dmaven.repo.local=$mavenRepository"
$env:Path = "$($javaDirectory.FullName)\bin;$($mavenDirectory.FullName)\bin;$env:Path"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"
