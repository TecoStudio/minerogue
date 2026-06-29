# Roguelike Paper Plugin Build Script
# Usage: .\build.ps1

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$srcDir = Join-Path $projectRoot "src\main\java"
$resDir = Join-Path $projectRoot "src\main\resources"
$classesDir = Join-Path $projectRoot "build\classes\java\main"
$resBuildDir = Join-Path $projectRoot "build\resources\main"
$libsDir = Join-Path $projectRoot "libs"
$outDir = Join-Path $projectRoot "build\libs"

# Read version from build.gradle
$version = "1.0.0"
$buildGradle = Join-Path $projectRoot "build.gradle"
if (Test-Path $buildGradle) {
    $content = Get-Content -Path $buildGradle -Raw
    if ($content -match 'version\s*=\s*["'']([^"'']+)["'']') {
        $version = $Matches[1]
    }
}

# Collect Java sources
$files = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
if ($files.Count -eq 0) {
    Write-Error "No Java sources found."
    exit 1
}

# Write sources list without BOM
$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding($false)
$sourcesFile = Join-Path $projectRoot "sources.txt"
[System.IO.File]::WriteAllLines($sourcesFile, $files, $Utf8NoBomEncoding)

# Build classpath
$jars = Get-ChildItem -Path $libsDir -Filter *.jar | Select-Object -ExpandProperty FullName
$classPath = ($jars -join ";")

# Compile
if (Test-Path $classesDir) {
    Remove-Item -Path $classesDir -Recurse -Force
}
if (Test-Path $resBuildDir) {
    Remove-Item -Path $resBuildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
Write-Host "Compiling Roguelike plugin (Java 17)..." -ForegroundColor Cyan
javac -cp $classPath -d $classesDir --release 17 "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed."
    exit 1
}

# Process resources
New-Item -ItemType Directory -Force -Path $resBuildDir | Out-Null
$pluginYml = Get-Content -Path (Join-Path $resDir "plugin.yml") -Raw
$pluginYml = $pluginYml.Replace('${version}', $version)
[System.IO.File]::WriteAllText((Join-Path $resBuildDir "plugin.yml"), $pluginYml, $Utf8NoBomEncoding)
Copy-Item -Path (Join-Path $resDir "config.yml") -Destination (Join-Path $resBuildDir "config.yml") -Force

# Package JAR
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$jarName = "roguelike-mod-$version.jar"
$jarPath = Join-Path $outDir $jarName
jar cf $jarPath -C $classesDir . -C $resBuildDir .

Write-Host "Build successful: $jarPath" -ForegroundColor Green
