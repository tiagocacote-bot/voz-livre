param([string]$PythonPath=$env:VOZLIVRE_PYTHON,[string]$ToolsDirectory=(Join-Path $PSScriptRoot ".toolchain"),[string]$SigningDirectory=(Join-Path $PSScriptRoot "signing"))
$ErrorActionPreference = 'Stop'
$tdRoot = $PSScriptRoot
$tdTools = $ToolsDirectory
$tdJdk = (Get-ChildItem (Join-Path $tdTools 'jdk') -Directory | Select-Object -First 1).FullName
$tdJava = Join-Path $tdJdk 'bin/java.exe'
$tdJavac = Join-Path $tdJdk 'bin/javac.exe'
$tdJar = Join-Path $tdJdk 'bin/jar.exe'
$tdKeytool = Join-Path $tdJdk 'bin/keytool.exe'
$tdBuild = Join-Path $tdRoot 'build'
$tdMain = Join-Path $tdRoot 'app/src/main'
$tdPython = $PythonPath
if (-not $tdPython) { $tdPython = (Get-Command python -ErrorAction Stop).Source }
function Invoke-Checked([string]$Program,[string[]]$Arguments) { & $Program @Arguments; if ($LASTEXITCODE -ne 0) { throw "Falha: $Program ($LASTEXITCODE)" } }
New-Item -ItemType Directory -Force (Join-Path $tdBuild 'classes'),(Join-Path $tdBuild 'dex'),(Join-Path $tdBuild 'generated'),$SigningDirectory | Out-Null
$tdTemp = Join-Path $tdBuild 'temp'
New-Item -ItemType Directory -Force $tdTemp | Out-Null
$env:TEMP = $tdTemp
$env:TMP = $tdTemp
$tdAapt = Join-Path $tdTools 'aapt2/aapt2.exe'
$tdManifest = Join-Path $tdBuild 'AndroidManifest.xml'
$tdManifestXml = [System.IO.File]::ReadAllText((Join-Path $tdMain 'AndroidManifest.xml')).Replace('<manifest ', '<manifest package="br.pessoal.vozlivre" ')
$tdVersion = ([xml]$tdManifestXml).manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
if (-not $tdVersion) { throw 'versionName ausente no manifesto' }
$tdApk = Join-Path $tdBuild "Voz-Livre-$tdVersion.apk"
[System.IO.File]::WriteAllText($tdManifest, $tdManifestXml)
Invoke-Checked $tdAapt @('compile','--dir',(Join-Path $tdMain 'res'),'-o',(Join-Path $tdBuild 'resources.zip'))
Invoke-Checked $tdAapt @('link','-I',(Join-Path $tdTools 'android.jar'),'--manifest',$tdManifest,'--java',(Join-Path $tdBuild 'generated'),'-o',(Join-Path $tdBuild 'base.apk'),(Join-Path $tdBuild 'resources.zip'))
$tdSources = @((Get-ChildItem (Join-Path $tdMain 'java') -Filter '*.java' -Recurse | ForEach-Object FullName)) + @((Get-ChildItem (Join-Path $tdBuild 'generated') -Filter '*.java' -Recurse | ForEach-Object FullName))
Invoke-Checked $tdJavac (@('-encoding','UTF-8','--release','8','-classpath',(Join-Path $tdTools 'android.jar'),'-d',(Join-Path $tdBuild 'classes')) + $tdSources)
Invoke-Checked $tdJar @('cf',(Join-Path $tdBuild 'classes.jar'),'-C',(Join-Path $tdBuild 'classes'),'.')
Invoke-Checked $tdJava @('-cp',(Join-Path $tdTools 'r8.jar'),'com.android.tools.r8.D8','--release','--min-api','26','--lib',(Join-Path $tdTools 'android.jar'),'--output',(Join-Path $tdBuild 'dex'),(Join-Path $tdBuild 'classes.jar'))
Invoke-Checked $tdPython @((Join-Path $tdRoot 'pack.py'))
$tdKey = Join-Path $SigningDirectory 'vozlivre.p12'
$tdPassword = Join-Path $SigningDirectory 'password.txt'
if (-not (Test-Path -LiteralPath $tdKey)) {
    $tdRandom = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($tdRandom)
    [System.IO.File]::WriteAllText($tdPassword,[Convert]::ToBase64String($tdRandom))
    Invoke-Checked $tdKeytool @('-genkeypair','-alias','vozlivre','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Voz Livre Pessoal','-storetype','PKCS12','-keystore',$tdKey,'-storepass:file',$tdPassword,'-keypass:file',$tdPassword)
}
Invoke-Checked $tdJavac @('--release','8','-classpath',(Join-Path $tdTools 'apksig.jar'),'-d',$tdBuild,(Join-Path $tdRoot 'Signer.java'))
$tdClasspath = $tdBuild + ';' + (Join-Path $tdTools 'apksig.jar')
Invoke-Checked $tdJava @('-cp',$tdClasspath,'Signer','sign',(Join-Path $tdBuild 'unsigned.apk'),$tdApk,$tdKey,$tdPassword)
Invoke-Checked $tdJava @('-cp',$tdClasspath,'Signer','verify',$tdApk)
Invoke-Checked $tdAapt @('dump','badging',$tdApk)
