Write-Host "[*] Stopping CampusTrade..." -ForegroundColor Yellow

Get-Job | Where-Object { $_.Name -like "CT-*" } | ForEach-Object {
    Write-Host "    Stopping $($_.Name)..." -ForegroundColor Gray
    Stop-Job -Job $_ -ErrorAction SilentlyContinue
    Remove-Job -Job $_ -ErrorAction SilentlyContinue
}

$p8080 = (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
if ($p8080) {
    Write-Host "    Releasing port 8080 (PID: $p8080)" -ForegroundColor Gray
    Stop-Process -Id $p8080 -Force -ErrorAction SilentlyContinue
}

$p5173 = (Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
if ($p5173) {
    Write-Host "    Releasing port 5173 (PID: $p5173)" -ForegroundColor Gray
    Stop-Process -Id $p5173 -Force -ErrorAction SilentlyContinue
}

Write-Host "    [+] Stopped" -ForegroundColor Green
