$jdk     = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
$m2      = "$env:USERPROFILE\.m2\repository\org\openjfx"
$modPath = "$m2\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar;" +
           "$m2\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar;" +
           "$m2\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;" +
           "$m2\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar"
$cp      = "target\classes"

Set-Location $PSScriptRoot

# Recompile si nécessaire
Write-Host ">> Compilation..." -ForegroundColor Cyan
$sources = Get-ChildItem "src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
& "$jdk\bin\javac.exe" --release 17 -encoding UTF-8 --module-path $modPath --add-modules javafx.controls,javafx.fxml -d target\classes @sources
if ($LASTEXITCODE -ne 0) { Write-Error "Compilation echouee."; exit 1 }

# Copier les ressources
Copy-Item "src\main\resources\*" "target\classes" -Recurse -Force

Write-Host ">> Lancement..." -ForegroundColor Green
& "$jdk\bin\java.exe" --module-path $modPath --add-modules javafx.controls,javafx.fxml -cp target\classes ma.bankdirect.App
