param([switch]$SinPruebas)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw 'No se encontró jpackage. Instala el JDK 21 o superior (https://adoptium.net) y vuelve a abrir la terminal.'
}

Write-Host '1/3 Compilando...' -ForegroundColor Cyan
$mvnArgs = @('-q', 'package')
if ($SinPruebas) { $mvnArgs += '-DskipTests' }
& .\mvnw.cmd @mvnArgs
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o alguna prueba.' }

$out = Join-Path $root 'target\instalador'
$in = Join-Path $out 'entrada'
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force $in | Out-Null
Copy-Item 'target\amoblador3d.jar' $in

$version = ([xml](Get-Content 'pom.xml')).project.version
$modules = 'java.base,java.desktop,java.sql,java.logging,jdk.jfr,jdk.unsupported,jdk.localedata,jdk.charsets'
$common = @(
    '--name', 'Space Cat',
    '--app-version', $version,
    '--vendor', 'FSHF',
    '--description', 'Diseñador de interiores 3D con medidas reales',
    '--input', $in,
    '--main-jar', 'amoblador3d.jar',
    '--main-class', 'pe.edu.unaj.amoblador3d.FSHFLauncher',
    '--icon', (Join-Path $root 'packaging\spacecat.ico'),
    '--add-modules', $modules,
    '--java-options', '--enable-native-access=ALL-UNNAMED',
    '--dest', $out
)

$wixDir = Join-Path $env:USERPROFILE 'tools\wix314'
if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -and (Test-Path (Join-Path $wixDir 'candle.exe'))) {
    $env:PATH = "$wixDir;$env:PATH"
}
$wix = (Get-Command wix.exe -ErrorAction SilentlyContinue) -or (Get-Command candle.exe -ErrorAction SilentlyContinue)

Write-Host '2/3 Creando la versión portátil (carpeta + .zip)...' -ForegroundColor Cyan
jpackage @common --type app-image
if ($LASTEXITCODE -ne 0) { throw 'jpackage no pudo crear la aplicación.' }
$zip = Join-Path $out "SpaceCat-$version-windows.zip"
Compress-Archive -Path (Join-Path $out 'Space Cat') -DestinationPath $zip
Copy-Item (Join-Path $root "target\amoblador3d.jar") (Join-Path $out "SpaceCat-$version.jar")

if ($wix) {
    Write-Host '3/3 Creando el instalador .exe (WiX)...' -ForegroundColor Cyan
    jpackage @common --type exe --win-menu --win-shortcut --win-dir-chooser --win-per-user-install `
        --win-menu-group 'Space Cat'
    if ($LASTEXITCODE -ne 0) { throw 'jpackage no pudo crear el instalador.' }
    Move-Item (Join-Path $out "Space Cat-$version.exe") (Join-Path $out "SpaceCat-$version.exe") -Force
} else {
    Write-Host '3/3 WiX no está instalado: no se crea el .exe (solo la versión portátil).' -ForegroundColor Yellow
}
Write-Host "Listo en $out :" -ForegroundColor Green
Get-ChildItem $out -File | ForEach-Object { Write-Host ("    {0}  ({1:N1} MB)" -f $_.Name, ($_.Length / 1MB)) }
Remove-Item -Recurse -Force $in
