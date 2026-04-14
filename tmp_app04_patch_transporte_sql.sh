echo 'kiriku#1060' | sudo -S -u bela.roger bash -lc "python3 - <<'PY'
from pathlib import Path
paths = [
    Path('/home/bela.roger/looker/src/sql/transporte.sql'),
    Path('/home/bela.roger/looker/sql/transporte.sql'),
]
old = \"'519553','481118')\"
new = \"'519553','481118','1058304','64749','1062294')\"
for p in paths:
    text = p.read_text()
    bak = p.with_name(p.name + '.bak.2026-03-25_novos_codtranspint_fix')
    bak.write_text(text)
    if new not in text:
        text = text.replace(old, new)
        p.write_text(text)
    final = p.read_text()
    print(p)
    print('contains 1058304', '1058304' in final)
    print('contains 64749', '64749' in final)
    print('contains 1062294', '1062294' in final)
PY"
