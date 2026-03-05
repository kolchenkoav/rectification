# PostgreSQL Backup Script

# Load .env variables
$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -notmatch '^#' -and $_ -match '=' } | ForEach-Object {
        $key, $value = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($key, $value, [EnvironmentVariableTarget]::Process)
    }
}

$containerName = if ($env:POSTGRES_HOST) { $env:POSTGRES_HOST } else { "rectification-db" }
$dbName = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { "rectification_db" }
$dbUser = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "postgres" }
$dbPassword = if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "password" }

$backupDir = Join-Path $PSScriptRoot "backups"
$date = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $backupDir "backup_$date.sql"

if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
}

Write-Host "Creating backup of database: $dbName"
Write-Host "Container: $containerName"

$env:PGPASSWORD = $dbPassword
$dockerCmd = "docker exec -i $containerName pg_dump -U $dbUser -Fc $dbName"

try {
    Invoke-Expression "$dockerCmd" | Out-File -FilePath $backupFile -Encoding utf8
    $exitCode = $LASTEXITCODE
} catch {
    $exitCode = 1
}

if ($exitCode -eq 0 -and (Test-Path $backupFile)) {
    Write-Host "Backup created: $backupFile" -ForegroundColor Green
    
    Get-ChildItem -Path $backupDir -Filter "backup_*.sql" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } | Remove-Item -Force
    Write-Host "Old backups (older than 30 days) removed"
} else {
    Write-Host "Error creating backup!" -ForegroundColor Red
    Write-Host "Check container name (current: $containerName)" -ForegroundColor Yellow
    exit 1
}
