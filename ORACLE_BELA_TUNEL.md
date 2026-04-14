# Oracle Bela Tunnel

Parametros fixados a partir da conexao usada no ambiente Bela:

- Host SSH: `bela-app02-oci.cldns.top`
- Porta SSH: `12022`
- Usuario SSH: `bela.jackson`
- Porta local do tunel: `11521`
- Destino Oracle remoto: `10.10.0.20:1521`
- Service Name: `skwpdb.sub03311532240.vncbela.oraclevcn.com`

Credenciais:

- SSH: Windows Credential Manager `servidor_oracle_bela_ssh`
- Oracle: Windows Credential Manager `servidor_oracle_bela_db`

Atalho para abrir o tunel:

```powershell
powershell -ExecutionPolicy Bypass -File ".\belamgz\scripts\abrir-tunel-oracle-bela.ps1"
```

Abrir tunel e ja entrar no SQL*Plus:

```powershell
powershell -ExecutionPolicy Bypass -File ".\belamgz\scripts\abrir-tunel-oracle-bela.ps1" -OpenSqlPlus
```

Substituir tunel existente na porta `11521`:

```powershell
powershell -ExecutionPolicy Bypass -File ".\belamgz\scripts\abrir-tunel-oracle-bela.ps1" -StopExistingOnPort
```

Observacao:

- Nao salvar senha em arquivo.
- O fluxo depende das credenciais ja gravadas no Windows Credential Manager.
