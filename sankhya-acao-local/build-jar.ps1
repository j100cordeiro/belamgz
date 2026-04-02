$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcRoot = Join-Path $root "src\\main\\java"
$classesDir = Join-Path $root "target\\classes"
$targetDir = Join-Path $root "target"
$jarPath = Join-Path $targetDir "sankhya-acao-altera-local-devolucao.jar"
$javaHome = "C:\\Program Files\\Java\\jdk1.8.0_202"
$javac = Join-Path $javaHome "bin\\javac.exe"
$jar = Join-Path $javaHome "bin\\jar.exe"
$cpEntries = @(
    "C:\\Users\\jacks\\.m2\\repository\\br\\com\\sankhya\\sankhya-extensions\\local\\sankhya-extensions-local.jar",
    "C:\\Users\\jacks\\.m2\\repository\\br\\com\\sankhya\\sankhya-jape\\local\\sankhya-jape-local.jar",
    "C:\\Users\\jacks\\.m2\\repository\\br\\com\\sankhya\\sankhya-modelcore\\local\\sankhya-modelcore-local.jar",
    "C:\\Users\\jacks\\OneDrive\\Desktop\\Java Bio\\valida_uf\\lib\\javax.ejb-api-3.2.2.jar"
)
$cp = $cpEntries -join ';'

if (!(Test-Path $javac)) {
    throw "javac nao encontrado em $javac"
}

if (!(Test-Path $jar)) {
    throw "jar nao encontrado em $jar"
}

foreach ($cpEntry in $cpEntries) {
    if (!(Test-Path $cpEntry)) {
        throw "Dependencia nao encontrada em $cpEntry"
    }
}

if (Test-Path $classesDir) {
    Remove-Item -LiteralPath $classesDir -Recurse -Force
}

New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

$javaFiles = Get-ChildItem -Path $srcRoot -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

if (-not $javaFiles) {
    throw "Nenhum arquivo .java encontrado em $srcRoot"
}

& $javac -encoding UTF-8 -source 1.8 -target 1.8 -cp $cp -d $classesDir $javaFiles

if ($LASTEXITCODE -ne 0) {
    throw "Falha na compilacao Java."
}

if (Test-Path $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}

Push-Location $classesDir
try {
    & $jar cf $jarPath .
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao gerar o JAR."
    }
} finally {
    Pop-Location
}

Write-Output $jarPath
