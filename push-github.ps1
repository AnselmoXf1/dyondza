Write-Host "=== Atualizando GitHub (AnselmoXf1/dyondza) ===" -ForegroundColor Cyan
git add .
git commit -m "feat: add auto fallback to h2 database when postgres is not reachable"
git push origin main
Write-Host "=== Atualizado com Sucesso! ===" -ForegroundColor Green
