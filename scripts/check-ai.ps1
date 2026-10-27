$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$envPath=Join-Path $projectRoot '.env'
if(!(Test-Path -LiteralPath $envPath)){throw 'Run setup.ps1 and save your OpenRouter API key in .env first.'}
$entry=Get-Content -LiteralPath $envPath | Where-Object {$_ -match '^OPENAI_API_KEY='} | Select-Object -Last 1
if(!$entry){throw 'OPENAI_API_KEY is missing from .env'}
$apiKey=($entry -split '=',2)[1].Trim()
if(!$apiKey){throw 'OPENAI_API_KEY is empty'}
$previousKey=$env:OPENAI_API_KEY
$previousLive=$env:NEXUS_LIVE_AI_TEST
$previousJava=$env:JAVA_HOME
Push-Location $projectRoot
try {
 $env:OPENAI_API_KEY=$apiKey
 $env:NEXUS_LIVE_AI_TEST='true'
 $localJdk=Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
 if($localJdk){$env:JAVA_HOME=$localJdk.FullName}
 $maven=Join-Path $projectRoot '.tools/apache-maven-3.9.11/bin/mvn.cmd'
 if(!(Test-Path -LiteralPath $maven)){$maven='mvn'}
 Write-Host 'Testing Java Spring AI with two small synthetic requests to the configured free Nemotron models. Credentials will not be printed.'
 & $maven -B -pl backend '-Dtest=OpenRouterLiveTest' test
 if($LASTEXITCODE -ne 0){throw 'Live AI check failed; inspect the test result without sharing credentials.'}
} finally {
 $env:OPENAI_API_KEY=$previousKey
 $env:NEXUS_LIVE_AI_TEST=$previousLive
 $env:JAVA_HOME=$previousJava
 $apiKey=$null
 Pop-Location
}
