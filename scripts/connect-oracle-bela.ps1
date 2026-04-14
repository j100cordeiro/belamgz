param(
    [int]$LocalPort = 11521,
    [string]$SshCredTarget = 'servidor_oracle_bela_ssh',
    [string]$DbCredTarget = 'servidor_oracle_bela_db',
    [string]$SshHost = 'bela-app02-oci.cldns.top',
    [int]$SshPort = 12022,
    [string]$RemoteDbHost = '10.10.0.20',
    [int]$RemoteDbPort = 1521,
    [string]$DbService = 'skwpdb.sub03311532240.vncbela.oraclevcn.com',
    [string]$PlinkPath = 'C:\Program Files\PuTTY\plink.exe',
    [switch]$OpenSqlPlus,
    [switch]$KeepTunnel,
    [switch]$StopExistingOnPort
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $PlinkPath)) {
    throw "Plink nao encontrado em: $PlinkPath"
}

if (-not (Get-Command sqlplus.exe -ErrorAction SilentlyContinue) -and $OpenSqlPlus) {
    throw 'sqlplus.exe nao encontrado no PATH. Remova -OpenSqlPlus ou configure o Oracle Client.'
}

if (-not ('WinCred.NativeMethods' -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

namespace WinCred {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct CREDENTIAL {
        public int Flags;
        public int Type;
        public string TargetName;
        public string Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public int CredentialBlobSize;
        public IntPtr CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public IntPtr Attributes;
        public string TargetAlias;
        public string UserName;
    }

    public static class NativeMethods {
        [DllImport("advapi32", CharSet = CharSet.Unicode, SetLastError = true)]
        public static extern bool CredRead(string target, int type, int reservedFlag, out IntPtr CredentialPtr);

        [DllImport("advapi32", SetLastError = true)]
        public static extern void CredFree([In] IntPtr cred);
    }
}
"@
}

function Get-StoredGenericCredential {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Target
    )

    $ptr = [IntPtr]::Zero
    $ok = [WinCred.NativeMethods]::CredRead($Target, 1, 0, [ref]$ptr)

    if (-not $ok -or $ptr -eq [IntPtr]::Zero) {
        $err = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
        throw "Nao foi possivel ler credencial '$Target' (erro Win32: $err)."
    }

    try {
        $cred = [Runtime.InteropServices.Marshal]::PtrToStructure($ptr, [type][WinCred.CREDENTIAL])
        $password = ''

        if ($cred.CredentialBlob -ne [IntPtr]::Zero -and $cred.CredentialBlobSize -gt 0) {
            $password = [Runtime.InteropServices.Marshal]::PtrToStringUni(
                $cred.CredentialBlob,
                [Math]::Floor($cred.CredentialBlobSize / 2)
            )
        }

        [PSCustomObject]@{
            Target   = $Target
            UserName = $cred.UserName
            Password = $password
        }
    }
    finally {
        [WinCred.NativeMethods]::CredFree($ptr)
    }
}

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Address,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutMs = 1000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect($Address, $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }
        $null = $client.EndConnect($iar)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Get-ListeningPid {
    param([int]$Port)

    $line = netstat -ano -p tcp | Select-String -Pattern ":$Port\s+.*LISTENING\s+\d+" | Select-Object -First 1
    if (-not $line) { return $null }

    $tokens = ($line.ToString() -replace '\s+', ' ').Trim().Split(' ')
    if ($tokens.Count -lt 5) { return $null }
    return [int]$tokens[-1]
}

$sshCred = Get-StoredGenericCredential -Target $SshCredTarget
$dbCred = Get-StoredGenericCredential -Target $DbCredTarget

if ([string]::IsNullOrWhiteSpace($sshCred.UserName) -or [string]::IsNullOrWhiteSpace($sshCred.Password)) {
    throw "Credencial SSH '$SshCredTarget' incompleta."
}

if ([string]::IsNullOrWhiteSpace($dbCred.UserName) -or [string]::IsNullOrWhiteSpace($dbCred.Password)) {
    throw "Credencial DB '$DbCredTarget' incompleta."
}

$existingPid = Get-ListeningPid -Port $LocalPort
if ($existingPid) {
    if ($StopExistingOnPort) {
        Stop-Process -Id $existingPid -Force
        Start-Sleep -Milliseconds 500
    }
    else {
        Write-Host "Ja existe processo escutando em 127.0.0.1:$LocalPort (PID $existingPid)."
        Write-Host 'Use -StopExistingOnPort para substituir o tunel atual.'
        return
    }
}

$plinkArgs = @(
    '-ssh',
    "$($sshCred.UserName)@$SshHost",
    '-P', "$SshPort",
    '-pw', $sshCred.Password,
    '-batch',
    '-N',
    '-L', "$LocalPort`:$RemoteDbHost`:$RemoteDbPort"
)

$plinkProc = Start-Process -FilePath $PlinkPath -ArgumentList $plinkArgs -PassThru -WindowStyle Hidden

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Milliseconds 250
    if ($plinkProc.HasExited) {
        throw "plink encerrou com codigo $($plinkProc.ExitCode)."
    }
    if (Test-TcpPort -Address '127.0.0.1' -Port $LocalPort -TimeoutMs 500) {
        $ready = $true
        break
    }
}

if (-not $ready) {
    try { Stop-Process -Id $plinkProc.Id -Force } catch {}
    throw "Tunel SSH nao ficou pronto em 127.0.0.1:$LocalPort."
}

Write-Host "Tunel ativo: 127.0.0.1:$LocalPort -> ${RemoteDbHost}:$RemoteDbPort (PID $($plinkProc.Id))"
Write-Host "DB user: $($dbCred.UserName)"
Write-Host "Service: $DbService"

if ($OpenSqlPlus) {
    $conn = "$($dbCred.UserName)/$($dbCred.Password)@//127.0.0.1:$LocalPort/$DbService"
    & sqlplus.exe -L $conn

    if (-not $KeepTunnel) {
        try {
            Stop-Process -Id $plinkProc.Id -Force
            Write-Host 'Tunel encerrado apos sair do SQL*Plus.'
        }
        catch {
            Write-Warning 'Nao foi possivel encerrar o tunel automaticamente.'
        }
    }
    else {
        Write-Host "Tunel mantido ativo (PID $($plinkProc.Id))."
    }
}
else {
    Write-Host ''
    Write-Host 'Para conectar manualmente via SQL*Plus:'
    Write-Host "sqlplus -L $($dbCred.UserName)/<senha>@//127.0.0.1:$LocalPort/$DbService"
    Write-Host "Para encerrar o tunel: Stop-Process -Id $($plinkProc.Id)"
}
