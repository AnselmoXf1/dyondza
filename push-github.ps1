Write-Host "=== Atualizando GitHub (AnselmoXf1/dyondza) ===" -ForegroundColor Cyan
git add .
git commit -m "fix: dockerfile context for render and cloud run"
git push origin main
Write-Host "=== Atualizado com Sucesso! ===" -ForegroundColor Green
