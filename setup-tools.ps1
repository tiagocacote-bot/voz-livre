param([Parameter(Mandatory=$true)][string]$ToolsDirectory)
$ErrorActionPreference='Stop'
Expand-Archive -LiteralPath (Join-Path $ToolsDirectory 'jdk.zip') -DestinationPath (Join-Path $ToolsDirectory 'jdk') -Force
$tdAaptZip=Join-Path $ToolsDirectory 'aapt2.zip'
Copy-Item -LiteralPath (Join-Path $ToolsDirectory 'aapt2.jar') -Destination $tdAaptZip -Force
Expand-Archive -LiteralPath $tdAaptZip -DestinationPath (Join-Path $ToolsDirectory 'aapt2') -Force
