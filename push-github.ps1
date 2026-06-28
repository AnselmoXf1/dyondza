Write-Host "=== Atualizando GitHub (AnselmoXf1/dyondza) ===" -ForegroundColor Cyan
git add .
git commit -m "fix: remove duplicate repositories block from backend build file"
git push origin main
Write-Host "=== Atualizado com Sucesso! ===" -ForegroundColor Green
