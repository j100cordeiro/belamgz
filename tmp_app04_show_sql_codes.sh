echo 'kiriku#1060' | sudo -S -u bela.roger bash -lc "python3 - <<'PY'
from pathlib import Path
text = Path('/home/bela.roger/looker/sql/transporte.sql').read_text()
for token in ('1058304','64749','1062294'):
    print(token, '->', token in text)
PY"
