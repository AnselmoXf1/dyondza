Write-Host "=== Atualizando GitHub (AnselmoXf1/dyondza) ===" -ForegroundColor Cyan
git add .
git commit -m "fix: skip android app compilation in docker build"
git push origin main
Write-Host "=== Atualizado com Sucesso! ===" -ForegroundColor Green
