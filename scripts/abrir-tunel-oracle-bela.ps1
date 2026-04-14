param(
    [switch]$OpenSqlPlus,
    [switch]$KeepTunnel,
    [switch]$StopExistingOnPort = $true
)

$baseScript = Join-Path $PSScriptRoot 'connect-oracle-bela.ps1'

if (-not (Test-Path $baseScript)) {
    throw "Script base nao encontrado: $baseScript"
}

& $baseScript `
    -LocalPort 11521 `
    -SshHost 'bela-app02-oci.cldns.top' `
    -SshPort 12022 `
    -RemoteDbHost '10.10.0.20' `
    -RemoteDbPort 1521 `
    -DbService 'skwpdb.sub03311532240.vncbela.oraclevcn.com' `
    -OpenSqlPlus:$OpenSqlPlus `
    -KeepTunnel:$KeepTunnel `
    -StopExistingOnPort:$StopExistingOnPort
