Write-Host "=== Enviando Projeto Dyondza para o GitHub ===" -ForegroundColor Cyan

# Inicializa o repositório se ainda não for um
if (-not (Test-Path ".git")) {
    git init
}

# Define a branch principal como 'main'
git branch -M main

# Remove o remoto 'origin' caso já exista para evitar erros e adiciona a nova URL
git remote remove origin 2>$null
git remote add origin https://github.com/AnselmoXf1/dyondza.git

Write-Host "`nAdicionando arquivos..." -ForegroundColor Yellow
git add .

Write-Host "`nCriando o commit..." -ForegroundColor Yellow
git commit -m "feat: reestruturacao do backend com Ktor, PostgreSQL, Redis e App Android"

Write-Host "`nEnviando para o GitHub (AnselmoXf1/dyondza)..." -ForegroundColor Yellow
git push -u origin main

Write-Host "`n=== Código enviado com sucesso para https://github.com/AnselmoXf1/dyondza.git ===" -ForegroundColor Green
