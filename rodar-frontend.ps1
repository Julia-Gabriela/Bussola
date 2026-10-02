if ($PSScriptRoot) {
    $raiz = $PSScriptRoot
} else {
    $raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
}

try {
    chcp 65001 | Out-Null
} catch {
}

$frontend = Join-Path $raiz "frontend"
$paginas = Join-Path $frontend "pages"

if (-not (Test-Path -LiteralPath $paginas)) {
    Write-Host "Não encontrei a pasta frontend\pages ao lado deste script."
    Write-Host "Pasta procurada: $paginas"
    exit 1
}

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Host "O comando python não foi encontrado neste PowerShell."
    Write-Host "Instale o Python 3 e abra o terminal de novo."
    exit 1
}

$base = "http://127.0.0.1:8765/pages"

Write-Host ""
Write-Host "========================================"
Write-Host "Bússola — Frontend"
Write-Host "========================================"
Write-Host ""
Write-Host "Frontend iniciado na porta 8765."
Write-Host ""
$inicial = Join-Path $paginas "index.html"
if (Test-Path -LiteralPath $inicial) {
    Write-Host "Abra o sistema no navegador:"
    Write-Host ""
    Write-Host "Página inicial:"
    Write-Host "$base/index.html"
    Write-Host ""
}
Write-Host "Páginas úteis para desenvolvimento:"
Write-Host ""

$uteis = @(
    @{ Arquivo = "cadastro.html"; Rotulo = "Cadastro:" },
    @{ Arquivo = "sobre.html"; Rotulo = "Sobre:" },
    @{ Arquivo = "home.html"; Rotulo = "Home provisória:" }
)

foreach ($item in $uteis) {
    $caminho = Join-Path $paginas $item.Arquivo
    if (Test-Path -LiteralPath $caminho) {
        Write-Host $item.Rotulo
        Write-Host "$base/$($item.Arquivo)"
        Write-Host ""
    }
}

Write-Host "Mantenha este terminal aberto."
Write-Host ""
Write-Host "Para parar o frontend:"
Write-Host "Ctrl + C"
Write-Host ""
Write-Host "========================================"
Write-Host ""

Set-Location -LiteralPath $frontend
python -m http.server 8765
