$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$envPath=Join-Path $projectRoot '.env'
function New-Secret { [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)).ToLowerInvariant() }
if(Test-Path -LiteralPath $envPath){
 $current=[IO.File]::ReadAllText($envPath)
 foreach($key in @('WORK_DB_PASSWORD','WORK_ADMIN_PASSWORD','OPERATIONS_TOKEN')){
  if($current -notmatch "(?m)^$key="){[IO.File]::AppendAllText($envPath,"`n$key=$(New-Secret)`n")}
 }
 Write-Host '.env preserved; missing task application credentials added.'; exit 0
}
$values=@("DB_PASSWORD=$(New-Secret)","DEMO_DB_PASSWORD=$(New-Secret)","SANDBOX_DB_PASSWORD=$(New-Secret)","NEXUS_USER=developer","NEXUS_PASSWORD=$(New-Secret)","OBSERVER_PASSWORD=$(New-Secret)","DEMO_TOKEN=$(New-Secret)",'AI_ENABLED=false','OPENAI_API_KEY=','CHAT_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free','EMBEDDING_MODEL=nvidia/nemotron-3.5-embed-1b:free','AI_BASE_URL=https://openrouter.ai/api')
[IO.File]::WriteAllLines($envPath,$values)
foreach($key in @('WORK_DB_PASSWORD','WORK_ADMIN_PASSWORD','OPERATIONS_TOKEN')){[IO.File]::AppendAllText($envPath,"$key=$(New-Secret)`n")}
Write-Host 'Created .env with random credentials. Read NEXUS_PASSWORD there to sign in as developer.'
Write-Host 'Start with: docker compose up --build -d'
