Write-Host "=== Atualizando GitHub (AnselmoXf1/dyondza) ===" -ForegroundColor Cyan
git add .
git commit -m "fix: inherit kotlin jvm plugin versions from root build file"
git push origin main
Write-Host "=== Atualizado com Sucesso! ===" -ForegroundColor Green
