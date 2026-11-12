# Quick kart - Local Full-Stack Runner (Windows PowerShell)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# Load variables from local .env if present
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim()
            [Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
}

Write-Host "Starting Quick kart Backend (Spring Boot + MySQL)..." -ForegroundColor White
$backendJar = Join-Path $root "backend\target\quickkart-backend-1.0.0.jar"
if (-not (Test-Path $backendJar)) {
    Write-Host "Building backend JAR first..."
    & "$root\.tools\apache-maven-3.9.9\bin\mvn.cmd" -f "$root\backend\pom.xml" package -DskipTests
}

Start-Process -FilePath "java" -ArgumentList "-jar", "`"$backendJar`"" -WorkingDirectory "$root\backend"
Start-Sleep -Seconds 3

Write-Host "Starting Quick kart Frontend (React + Vite)..." -ForegroundColor White
Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory "$root\frontend"

Write-Host ""
Write-Host "Quick kart is starting:" -ForegroundColor Green
Write-Host "  Storefront : http://localhost:5173"
Write-Host "  Backend API: http://localhost:8080"
