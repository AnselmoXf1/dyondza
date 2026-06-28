Write-Host "=== Atualizando GitHub (AnselmoXf1/dyondza) ===" -ForegroundColor Cyan
git add .
git commit -m "fix: replace deprecated openjdk base image with eclipse-temurin"
git push origin main
Write-Host "=== Atualizado com Sucesso! ===" -ForegroundColor Green
