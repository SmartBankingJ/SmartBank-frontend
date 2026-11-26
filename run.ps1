param([string]$JavaHome, [string]$JavaFxLib, [string]$GsonJar)
$launcher = Join-Path $PSScriptRoot '..\SmartBank-App\scripts\run-desktop.ps1'
if (Test-Path -LiteralPath $launcher) {
    & $launcher @PSBoundParameters
} else {
    Push-Location $PSScriptRoot
    try {
        if ($JavaHome) { $env:JAVA_HOME = $JavaHome; $env:Path = (Join-Path $JavaHome 'bin') + ';' + $env:Path }
        & mvn.cmd javafx:run
        if ($LASTEXITCODE -ne 0) { throw 'Demarrage JavaFX echoue.' }
    } finally { Pop-Location }
}