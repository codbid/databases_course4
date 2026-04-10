param(
    [string]$DataFile = ""
)

$defaultPath = Join-Path $PSScriptRoot "..\data\book_events.ndjson"
if ([string]::IsNullOrWhiteSpace($DataFile)) {
    $DataFile = $defaultPath
}

if (-not (Test-Path $DataFile)) {
    Write-Error "Data file not found: $DataFile"
    exit 1
}

$topic = "library.book-events"

docker exec kafka1 kafka-topics `
    --create `
    --if-not-exists `
    --topic $topic `
    --bootstrap-server kafka1:9092 `
    --partitions 3 `
    --replication-factor 1 | Out-Null

Get-Content $DataFile | docker exec -i kafka1 kafka-console-producer `
    --bootstrap-server kafka1:9092 `
    --topic $topic | Out-Null

$rows = (Get-Content $DataFile | Measure-Object -Line).Lines
Write-Host "Rows in file: $rows"
Write-Host "Rows currently in ClickHouse:"

docker exec library_clickhouse clickhouse-client `
    --user default `
    --password clickhouse `
    --query "SELECT count() FROM library_analytics.book_events WHERE source = 'library-backend';"
