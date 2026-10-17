param([string]$ToolsDirectory=(Join-Path (Split-Path $PSScriptRoot -Parent) '.toolchain'),[string]$DependenciesDirectory=(Join-Path (Split-Path $PSScriptRoot -Parent) 'build/robo-deps'),[string]$SampleFile=(Join-Path (Split-Path $PSScriptRoot -Parent) '.test-fixtures/sample.spb'))
$ErrorActionPreference='Stop' 
$testRoot=$PSScriptRoot
$appRoot=Split-Path $testRoot -Parent
$toolsRoot=$ToolsDirectory
$jdkRoot=(Get-ChildItem (Join-Path $toolsRoot 'jdk') -Directory | Select-Object -First 1).FullName
$testBuild=Join-Path $appRoot 'build/runtime-tests'
$testDeps=$DependenciesDirectory
New-Item -ItemType Directory -Force (Join-Path $testBuild 'com/android/tools') | Out-Null
New-Item -ItemType Directory -Force (Join-Path (Split-Path $appRoot -Parent) 'rt') | Out-Null
$testCp=(Join-Path $appRoot 'build/classes')+';'+(Join-Path $toolsRoot 'android.jar')+';'+$testDeps+'/*'
& (Join-Path $jdkRoot 'bin/javac.exe') -encoding UTF-8 --release 8 -cp $testCp -d $testBuild (Join-Path $testRoot 'RuntimeTest.java') (Join-Path $testRoot 'RenderTest.java')
if($LASTEXITCODE -ne 0){throw 'Falha na compilação dos testes'}
$mainRoot=Join-Path $appRoot 'app/src/main'
$properties=@(
 ('android_merged_manifest='+((Join-Path $mainRoot 'AndroidManifest.xml').Replace('\','/')))
 ('android_merged_resources='+((Join-Path $mainRoot 'res').Replace('\','/')))
 ('android_merged_assets='+((Join-Path $mainRoot 'assets').Replace('\','/')))
 ('android_resource_apk='+((Join-Path $appRoot 'build/unsigned.apk').Replace('\','/')))
 'android_custom_package=br.pessoal.vozlivre'
)
[System.IO.File]::WriteAllLines((Join-Path $testBuild 'com/android/tools/test_config.properties'),$properties)
$runtimeCp=$testBuild+';'+(Join-Path $appRoot 'build/classes')+';'+(Join-Path $toolsRoot 'android.jar')+';'+$testDeps+'/*'
$runtimeArgs=@(('-Duser.home='+$testBuild),('-Djava.io.tmpdir='+(Join-Path (Split-Path $appRoot -Parent) 'rt')),'--add-opens=java.base/java.lang=ALL-UNNAMED','--add-opens=java.base/java.util=ALL-UNNAMED','--add-opens=java.base/java.io=ALL-UNNAMED','--add-opens=java.base/java.net=ALL-UNNAMED','--add-opens=java.base/java.security=ALL-UNNAMED','--add-opens=java.base/java.text=ALL-UNNAMED','--add-opens=java.base/jdk.internal.access=ALL-UNNAMED','--add-opens=java.desktop/java.awt.font=ALL-UNNAMED','--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED','-Drobolectric.dependency.repo.url=https://repo.maven.apache.org/maven2',('-Dmaven.repo.local='+(Join-Path $toolsRoot 'm2')),('-Dsnap.sample='+$SampleFile),('-Drender.dir='+(Join-Path $appRoot 'build')),'-cp',$runtimeCp,'org.junit.runner.JUnitCore','br.pessoal.vozlivre.RuntimeTest','br.pessoal.vozlivre.RenderTest')
& (Join-Path $jdkRoot 'bin/java.exe') @runtimeArgs
if($LASTEXITCODE -ne 0){throw 'Testes Android falharam'}
