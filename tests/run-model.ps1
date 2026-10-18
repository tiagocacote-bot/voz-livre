param([string]$ToolsDirectory=(Join-Path (Split-Path $PSScriptRoot -Parent) '.toolchain'))
$ErrorActionPreference='Stop'
$appRoot=Split-Path $PSScriptRoot -Parent
$jdkRoot=(Get-ChildItem (Join-Path $ToolsDirectory 'jdk') -Directory | Select-Object -First 1).FullName
$out=Join-Path $appRoot 'build/model-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$sources=@('Model.java','ScanEngine.java','Predictor.java','Grammar.java','I18n.java','Crypto.java') | ForEach-Object { Join-Path $appRoot ('app/src/main/java/br/pessoal/vozlivre/'+$_) }
& (Join-Path $jdkRoot 'bin/javac.exe') -encoding UTF-8 --release 8 -cp (Join-Path $ToolsDirectory 'json.jar') -d $out @sources (Join-Path $PSScriptRoot 'ModelTest.java')
if($LASTEXITCODE -ne 0){throw 'Model test compilation failed'}
& (Join-Path $jdkRoot 'bin/java.exe') -cp ($out+';'+(Join-Path $ToolsDirectory 'json.jar')) br.pessoal.vozlivre.ModelTest (Join-Path $appRoot 'app/src/main/assets/default.json')
if($LASTEXITCODE -ne 0){throw 'Model tests failed'}
