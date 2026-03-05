# PostgreSQL Restore Script

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

Write-Host "=== PostgreSQL Database Restore ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Container: $containerName"
Write-Host "Database: $dbName"

if (-not (Test-Path $backupDir)) {
    Write-Host "Backup directory not found: $backupDir" -ForegroundColor Red
    exit 1
}

$backups = Get-ChildItem -Path $backupDir -Filter "backup_*.sql" | Sort-Object LastWriteTime -Descending

if ($backups.Count -eq 0) {
    Write-Host "No backups found!" -ForegroundColor Red
    exit 1
}

Write-Host "Available backups:" -ForegroundColor Yellow
for ($i = 0; $i -lt $backups.Count; $i++) {
    Write-Host "[$($i + 1)] $($backups[$i].Name) - $($backups[$i].LastWriteTime)"
}

Write-Host ""
$choice = Read-Host "Enter backup number to restore (or press Enter for latest)"

if ([string]::IsNullOrWhiteSpace($choice)) {
    $backupFile = $backups[0].FullName
} else {
    $index = [int]$choice - 1
    if ($index -ge 0 -and $index -lt $backups.Count) {
        $backupFile = $backups[$index].FullName
    } else {
        Write-Host "Invalid backup number!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Selected backup: $backupFile"
Write-Host ""

$confirm = Read-Host "WARNING: Current data will be deleted! Continue? (yes/no)"

if ($confirm -ne "yes") {
    Write-Host "Cancelled by user."
    exit 0
}

$tempFile = "temp_restore.sql"
$containerPath = "/tmp/$tempFile"

Write-Host "Copying backup to container..."
docker cp $backupFile "$containerName`:$containerPath"

Write-Host "Dropping existing database..."
$env:PGPASSWORD = $dbPassword
docker exec -i $containerName psql -U $dbUser -c "DROP DATABASE IF EXISTS $dbName;" 2>$null

Write-Host "Creating new database..."
docker exec -i $containerName psql -U $dbUser -c "CREATE DATABASE $dbName;" 2>$null

Write-Host "Restoring from backup..."
docker exec -i $containerName sh -c "PGPASSWORD=$dbPassword pg_restore -U $dbUser -d $dbName -c < $containerPath" 2>$null

if ($LASTEXITCODE -eq 0) {
    Write-Host "Cleaning up..."
    docker exec -i $containerName rm -f $containerPath 2>$null
    
    Write-Host ""
    Write-Host "=== Database restored successfully! ===" -ForegroundColor Green
} else {
    Write-Host "Error restoring database!" -ForegroundColor Red
    exit 1
}
