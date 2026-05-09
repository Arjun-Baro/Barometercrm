# ═══════════════════════════════════════════════════════════════════════════
#  Barometer CRM — Start Script
#  Usage: .\run.ps1
#  Reads .env, builds (optional) and starts the Spring Boot backend.
# ═══════════════════════════════════════════════════════════════════════════

param(
    [switch]$Build   # Pass -Build to rebuild JAR before starting
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# ── 1. Load .env ─────────────────────────────────────────────────────────
$EnvFile = Join-Path $ScriptDir ".env"
if (-Not (Test-Path $EnvFile)) {
    Write-Host "ERROR: .env file not found at $EnvFile" -ForegroundColor Red
    Write-Host "Copy .env.example to .env and fill in your credentials." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "  Loading .env ..." -ForegroundColor Cyan
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    # Skip comments and blank lines
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    # Parse KEY=VALUE
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $key   = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    # Use $env: so child processes (java) actually inherit the variable
    Set-Item -Path "Env:$key" -Value $value
    # Mask secrets in output
    if ($key -match "SECRET|KEY|PASSWORD|URI") {
        $masked = if ($value.Length -gt 8) { $value.Substring(0, 4) + "****" + $value.Substring($value.Length - 4) } else { "****" }
        Write-Host "  $key = $masked" -ForegroundColor DarkGray
    } else {
        Write-Host "  $key = $value" -ForegroundColor DarkGray
    }
}

# ── 2. Validate required vars ────────────────────────────────────────────
$required = @("MONGODB_URI", "JWT_SECRET")
$missing  = @()
foreach ($var in $required) {
    $val = [System.Environment]::GetEnvironmentVariable($var, "Process")
    if (-not $val -or $val -match "^REPLACE_ME|^USERNAME:PASSWORD") {
        $missing += $var
    }
}

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "  WARNING: The following required variables are not set:" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "    - $_" -ForegroundColor Yellow }
    Write-Host "  Edit barometer-crm-backend\.env and restart." -ForegroundColor Yellow
    Write-Host ""
    $confirm = Read-Host "  Continue anyway? (y/N)"
    if ($confirm -ne "y" -and $confirm -ne "Y") { exit 1 }
}

# ── 3. Optional rebuild ──────────────────────────────────────────────────
$Jar = Join-Path $ScriptDir "target\barometer-crm-1.0.0.jar"
if ($Build -or -Not (Test-Path $Jar)) {
    Write-Host ""
    Write-Host "  Building JAR (mvn clean package -DskipTests) ..." -ForegroundColor Cyan
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  BUILD FAILED" -ForegroundColor Red
        exit 1
    }
}

# ── 4. Start the app ─────────────────────────────────────────────────────
$port = [System.Environment]::GetEnvironmentVariable("SERVER_PORT", "Process")
if (-not $port) { $port = "8080" }

Write-Host ""
Write-Host "  Starting Barometer CRM Backend on port $port ..." -ForegroundColor Green
Write-Host "  API:    http://localhost:$port/api" -ForegroundColor White
Write-Host "  Health: http://localhost:$port/health" -ForegroundColor White
Write-Host "  Press Ctrl+C to stop." -ForegroundColor DarkGray
Write-Host ""

# Read env vars explicitly to pass as JVM -D properties (avoids env inheritance issues)
$mongoUri  = [System.Environment]::GetEnvironmentVariable("MONGODB_URI",      "Process")
$jwtSecret = [System.Environment]::GetEnvironmentVariable("JWT_SECRET",       "Process")
$claudeKey = [System.Environment]::GetEnvironmentVariable("CLAUDE_API_KEY",   "Process")
$vapidPub  = [System.Environment]::GetEnvironmentVariable("VAPID_PUBLIC_KEY", "Process")
$vapidPriv = [System.Environment]::GetEnvironmentVariable("VAPID_PRIVATE_KEY","Process")
$corsOrig  = [System.Environment]::GetEnvironmentVariable("CORS_ORIGINS",     "Process")

$javaArgs = @(
    "-Djdk.tls.client.protocols=TLSv1.2",
    "-Djsse.enableSNIExtension=true",
    "-Djavax.net.ssl.trustStoreType=PKCS12",
    "-Dspring.data.mongodb.uri=$mongoUri",
    "-Djwt.secret=$jwtSecret",
    "-Dclaude.api-key=$claudeKey",
    "-Dclaude.model=claude-haiku-4-5-20251001",
    "-Dvapid.public-key=$vapidPub",
    "-Dvapid.private-key=$vapidPriv",
    "-Dcors.origins=$corsOrig",
    "-Dserver.port=$port",
    "-jar", $Jar
)
& java @javaArgs
