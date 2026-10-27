$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$envPath=Join-Path $projectRoot '.env'
if(Test-Path -LiteralPath $envPath){Write-Host '.env already exists; existing credentials preserved.'; exit 0}
function New-Secret { [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)).ToLowerInvariant() }
$values=@("DB_PASSWORD=$(New-Secret)","DEMO_DB_PASSWORD=$(New-Secret)","SANDBOX_DB_PASSWORD=$(New-Secret)","NEXUS_USER=developer","NEXUS_PASSWORD=$(New-Secret)","OBSERVER_PASSWORD=$(New-Secret)","DEMO_TOKEN=$(New-Secret)",'AI_ENABLED=false','OPENAI_API_KEY=','CHAT_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free','EMBEDDING_MODEL=nvidia/nemotron-3.5-embed-1b:free','AI_BASE_URL=https://openrouter.ai/api')
[IO.File]::WriteAllLines($envPath,$values)
Write-Host 'Created .env with random credentials. Read NEXUS_PASSWORD there to sign in as developer.'
Write-Host 'Start with: docker compose up --build -d'
