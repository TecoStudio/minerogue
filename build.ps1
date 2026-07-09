# Roguelike Paper Plugin Build Script
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 [-TimeoutSeconds 180]

param(
    [ValidateRange(1, 86400)]
    [int] $TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$gradleBat = Join-Path $projectRoot "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradleBat)) {
    [Console]::Error.WriteLine("Gradle wrapper not found: $gradleBat")
    exit 1
}

$arguments = @("--no-daemon", "clean", "build")
Write-Host "Building Roguelike plugin with Gradle: $($arguments -join ' ')" -ForegroundColor Cyan
Write-Host "Timeout: $TimeoutSeconds seconds" -ForegroundColor DarkGray

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $env:ComSpec
$startInfo.Arguments = "/d /c call `"$gradleBat`" $($arguments -join ' ')"
$startInfo.WorkingDirectory = $projectRoot
$startInfo.UseShellExecute = $false

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$process.Start() | Out-Null

if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
    [Console]::Error.WriteLine("Build timed out after $TimeoutSeconds seconds. Terminating Gradle process tree $($process.Id).")
    try {
        & taskkill.exe /PID $process.Id /T /F | Out-Null
        $process.WaitForExit()
    } catch {
        Write-Warning "Failed to terminate Gradle process $($process.Id): $($_.Exception.Message)"
    }
    exit 124
}

if ($process.ExitCode -ne 0) {
    [Console]::Error.WriteLine("Gradle build failed with exit code $($process.ExitCode).")
    exit $process.ExitCode
}

Write-Host "Build successful. Artifacts are in: $(Join-Path $projectRoot 'build\libs')" -ForegroundColor Green
exit 0
