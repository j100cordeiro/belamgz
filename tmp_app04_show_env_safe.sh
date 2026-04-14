echo 'kiriku#1060' | sudo -S -u bela.roger bash -lc "python3 - <<'PY'
from pathlib import Path
for line in Path('/home/bela.roger/looker/.env').read_text().splitlines():
    if 'SANKHYA' in line or line.startswith(('BM_MYSQL_USER=','BM_MYSQL_HOST=','BM_MYSQL_DBASE=')):
        print(line)
PY"
