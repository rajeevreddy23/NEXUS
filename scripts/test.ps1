$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
 $localJdk=Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
 if($localJdk){$env:JAVA_HOME=$localJdk.FullName}
 $maven=Join-Path $projectRoot '.tools/apache-maven-3.9.11/bin/mvn.cmd'
 if(!(Test-Path -LiteralPath $maven)){$maven='mvn'}
 & $maven -B verify
 if($LASTEXITCODE -ne 0){throw 'Java verification failed'}
 Push-Location frontend
 try {npm.cmd ci --no-fund --no-audit; if($LASTEXITCODE -ne 0){throw 'Install failed'}; npm.cmd run build; if($LASTEXITCODE -ne 0){throw 'Frontend build failed'}; npx.cmd playwright install chromium; if($LASTEXITCODE -ne 0){throw 'Browser install failed'}; npm.cmd test; if($LASTEXITCODE -ne 0){throw 'Frontend tests failed'}} finally {Pop-Location}
} finally {Pop-Location}
