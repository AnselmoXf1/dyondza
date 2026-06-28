param(
    [Parameter(Mandatory=$true)]
    [string]$ProjectId,

    [string]$Region = "us-central1",
    [string]$ServiceName = "dyondza-backend"
)

Write-Host "=== Deploy do Backend Dyondza para o Google Cloud Run ===" -ForegroundColor Cyan
Write-Host "Projeto GCP: $ProjectId" -ForegroundColor Yellow
Write-Host "Regiao: $Region" -ForegroundColor Yellow

# Verifica se o gcloud está instalado
if (-not (Get-Command "gcloud" -ErrorAction SilentlyContinue)) {
    Write-Host "ERRO: O Google Cloud SDK (gcloud) nao foi encontrado no seu PATH." -ForegroundColor Red
    Write-Host "DICA: Abra o Google Cloud Shell no navegador em https://console.cloud.google.com ou instale o Google Cloud SDK." -ForegroundColor White
    exit 1
}

Write-Host "`n1. Configurando o projeto GCP atual..." -ForegroundColor Green
gcloud config set project $ProjectId

Write-Host "`n2. Habilitando APIs necessárias (Cloud Run, Cloud Build, Artifact Registry)..." -ForegroundColor Green
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com

Write-Host "`n3. Submetendo o build para o Google Cloud Build..." -ForegroundColor Green
gcloud builds submit --config cloudbuild.yaml .

Write-Host "`n=== DEPLOY CONCLUÍDO COM SUCESSO! ===" -ForegroundColor Cyan
Write-Host "Consulte a URL gerada pelo Cloud Run e atualize a variável BASE_URL no arquivo DyondzaApiService.kt do aplicativo móvel!" -ForegroundColor Yellow
