echo ''kiriku#1060'' | sudo -S -u bela.roger bash -lc "python3 - <<'PY'
from pathlib import Path
p = Path('/home/bela.roger/looker/src/sql/transporte.sql')
print('exists', p.exists())
text = p.read_text() if p.exists() else ''
for token in ('1058304','64749','1062294'):
    print(token, '->', token in text)
print('tabelas.txt ->')
print(Path('/home/bela.roger/looker/src/sql/tabelas.txt').read_text())
PY"
