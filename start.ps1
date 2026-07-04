<#
.SYNOPSIS
    CampusTrade one-click startup script
.PARAMETER Dev
    Use H2 in-memory database (no MySQL needed)
.EXAMPLE
    .\start.ps1 -Dev
#>
param([switch]$Dev = $false)

$rootDir = $PSScriptRoot

function Test-Port($port) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        if ($tcp.ConnectAsync("127.0.0.1", $port).Wait(2000)) {
            $tcp.Close(); return $true
        }
        $tcp.Close(); return $false
    } catch { return $false }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CampusTrade - Startup" -ForegroundColor Cyan
if ($Dev) { Write-Host "  Mode: H2 (no MySQL needed)" -ForegroundColor Magenta }
else       { Write-Host "  Mode: MySQL" -ForegroundColor Magenta }
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# === 1. Env Check ===
Write-Host "[*] Checking environment..." -ForegroundColor Yellow

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[!] JDK 17+ required" -ForegroundColor Red; exit 1
}
Write-Host "    [+] Java" -ForegroundColor Green

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "[!] Node.js required" -ForegroundColor Red; exit 1
}
Write-Host "    [+] Node.js" -ForegroundColor Green

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "[!] Maven required" -ForegroundColor Red; exit 1
}
Write-Host "    [+] Maven" -ForegroundColor Green
Write-Host ""

# === 2. Database ===
if ($Dev) {
    Write-Host "[*] H2 in-memory database mode" -ForegroundColor Yellow
    $profile = "dev"
} else {
    Write-Host "[*] Checking MySQL..." -ForegroundColor Yellow
    $mysqlExe = $null
    foreach ($p in @("C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe", "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe")) {
        if (Test-Path $p) { $mysqlExe = $p; break }
    }
    $mysqlOk = $false
    if ($mysqlExe) {
        $env:MYSQL_PWD = "root"
        $result = & $mysqlExe -u root -e "SELECT 1" 2>&1
        if ($LASTEXITCODE -eq 0) {
            $mysqlOk = $true
            Write-Host "    [+] MySQL connected" -ForegroundColor Green
            & $mysqlExe -u root -e "CREATE DATABASE IF NOT EXISTS campus_trade DEFAULT CHARACTER SET utf8mb4" 2>&1 | Out-Null
        }
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    if ($mysqlOk) {
        $profile = "prod"
    } else {
        Write-Host "    [!] MySQL not available, fallback to H2" -ForegroundColor Yellow
        $profile = "dev"
    }
}
Write-Host ""

# === 3. Frontend Deps ===
Write-Host "[*] Checking frontend deps..." -ForegroundColor Yellow
$webDir = Join-Path $rootDir "campus-trade-web"
if (-not (Test-Path (Join-Path $webDir "node_modules"))) {
    Write-Host "    Installing..." -ForegroundColor Gray
    Push-Location $webDir; npm install 2>&1 | Out-Null; Pop-Location
    if (-not (Test-Path (Join-Path $webDir "node_modules"))) { Write-Host "[!] npm install failed" -ForegroundColor Red; exit 1 }
}
Write-Host "    [+] Ready" -ForegroundColor Green
Write-Host ""

# === 4. Build Backend ===
Write-Host "[*] Building backend..." -ForegroundColor Yellow
$serverDir = Join-Path $rootDir "campus-trade-server"
Push-Location $serverDir
mvn package -DskipTests -q 2>&1 | Out-Null
$buildOk = ($LASTEXITCODE -eq 0)
Pop-Location
if (-not $buildOk) { Write-Host "[!] Build failed" -ForegroundColor Red; exit 1 }
Write-Host "    [+] Build done" -ForegroundColor Green

# === 5. Free Ports ===
.\stop.ps1 2>&1 | Out-Null
Start-Sleep 2

# === 6. Start Backend ===
Write-Host "[*] Starting backend (port 8080)..." -ForegroundColor Yellow
$backendLog = Join-Path $serverDir "server.log"
$jarPath = Join-Path $serverDir "target\campus-trade-server-1.0.0.jar"

$backendProc = Start-Process cmd -ArgumentList "/c", "java -jar `"$jarPath`" --spring.profiles.active=$profile >> `"$backendLog`" 2>&1" `
    -NoNewWindow -PassThru

Write-Host "    Waiting for backend (PID: $($backendProc.Id))..." -ForegroundColor Gray
$ready = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Port 8080) { $ready = $true; Write-Host "    [+] Backend ready (${i}s)" -ForegroundColor Green; break }
    if ($i % 15 -eq 0) { Write-Host "    ... ${i}s" -ForegroundColor Gray }
}
if (-not $ready) {
    Write-Host "    [!] Backend timeout, check: $backendLog" -ForegroundColor Red
}
Write-Host ""

# === 7. Start Frontend ===
Write-Host "[*] Starting frontend (port 5173)..." -ForegroundColor Yellow
$viteLog = Join-Path $webDir "vite.log"

$frontendProc = Start-Process cmd -ArgumentList "/c", "npx vite --host >> `"$viteLog`" 2>&1" `
    -NoNewWindow -PassThru -WorkingDirectory $webDir

Start-Sleep 4
if (Test-Port 5173) {
    Write-Host "    [+] Frontend ready" -ForegroundColor Green
} else {
    Write-Host "    [!] Frontend may still be starting (PID: $($frontendProc.Id))" -ForegroundColor Yellow
}
Write-Host ""

# === 8. Done ===
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CampusTrade is running!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Frontend : http://localhost:5173" -ForegroundColor Green
Write-Host "  API      : http://localhost:8080" -ForegroundColor Green
if ($profile -eq "dev") {
    Write-Host "  H2 Console: http://localhost:8080/h2-console" -ForegroundColor Gray
}
Write-Host ""
Write-Host "  Backend  PID: $($backendProc.Id)  Log: $backendLog" -ForegroundColor Gray
Write-Host "  Frontend PID: $($frontendProc.Id)  Log: $viteLog" -ForegroundColor Gray
Write-Host ""
Write-Host "  Stop: .\stop.ps1" -ForegroundColor Yellow
Write-Host ""

try { Start-Process "http://localhost:5173" } catch {}
