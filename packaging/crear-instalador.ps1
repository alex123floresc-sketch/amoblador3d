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
    '--name', 'Amoblador3D',
    '--app-version', $version,
    '--vendor', 'UNAJ',
    '--description', 'Diseñador de interiores 3D con medidas reales',
    '--input', $in,
    '--main-jar', 'amoblador3d.jar',
    '--main-class', 'pe.edu.unaj.amoblador3d.FSHFLauncher',
    '--icon', (Join-Path $root 'packaging\amoblador3d.ico'),
    '--add-modules', $modules,
    '--java-options', '--enable-native-access=ALL-UNNAMED',
    '--dest', $out
)

$wix = (Get-Command wix.exe -ErrorAction SilentlyContinue) -or (Get-Command candle.exe -ErrorAction SilentlyContinue)
if ($wix) {
    Write-Host '2/3 Creando el instalador .exe (WiX)...' -ForegroundColor Cyan
    jpackage @common --type exe --win-menu --win-shortcut --win-dir-chooser --win-per-user-install `
        --win-menu-group 'Amoblador3D'
    if ($LASTEXITCODE -ne 0) { throw 'jpackage no pudo crear el instalador.' }
    Write-Host "3/3 Listo: $out\Amoblador3D-$version.exe" -ForegroundColor Green
} else {
    Write-Host '2/3 WiX no está instalado: se crea la versión portátil (carpeta + .zip)...' -ForegroundColor Yellow
    jpackage @common --type app-image
    if ($LASTEXITCODE -ne 0) { throw 'jpackage no pudo crear la aplicación.' }
    $zip = Join-Path $out "Amoblador3D-$version-windows.zip"
    Compress-Archive -Path (Join-Path $out 'Amoblador3D') -DestinationPath $zip
    Write-Host "3/3 Listo:" -ForegroundColor Green
    Write-Host "    Aplicación: $out\Amoblador3D\Amoblador3D.exe"
    Write-Host "    Para compartir: $zip"
}
Remove-Item -Recurse -Force $in
