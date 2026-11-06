#!/usr/bin/env bash
set -euo pipefail
API="${API_URL:-http://localhost:8080}"
for cmd in curl jq python3; do command -v "$cmd" >/dev/null || { echo "Instale: $cmd"; exit 2; }; done
ID="$(python3 -c 'import uuid; print(uuid.uuid4())')"
curl -fsS -X POST "$API/api/accounts" -H 'Content-Type: application/json' \
  -d "{\"accountId\":\"$ID\",\"owner\":\"Teste concorrencia\"}" >/dev/null
curl -fsS -X POST "$API/api/accounts/$ID/deposits" -H 'Content-Type: application/json' \
  -d '{"amount":100.00,"reference":"FUNDING"}' >/dev/null
withdraw() {
  curl -sS -o /dev/null -w '%{http_code}' -X POST "$API/api/accounts/$ID/withdrawals" \
    -H 'Content-Type: application/json' -d '{"amount":80.00,"reference":"RACE"}'
}
withdraw > /tmp/es-race-1.$$ & P1=$!
withdraw > /tmp/es-race-2.$$ & P2=$!
wait "$P1"; wait "$P2"
S1="$(cat /tmp/es-race-1.$$)"; S2="$(cat /tmp/es-race-2.$$)"
rm -f /tmp/es-race-1.$$ /tmp/es-race-2.$$
echo "HTTP statuses: $S1 e $S2"
[[ "$S1" == "202" && "$S2" != "202" || "$S2" == "202" && "$S1" != "202" ]] || {
  echo "ERRO: apenas um saque deve ser aceito"; exit 1;
}
BALANCE="$(curl -fsS "$API/api/accounts/$ID/state" | jq -r '.balance')"
[[ "$BALANCE" == "20.00" || "$BALANCE" == "20.0" || "$BALANCE" == "20" ]] || {
  echo "ERRO: saldo inesperado: $BALANCE"; exit 1;
}
echo "[PASS] Sem saldo negativo; optimistic concurrency protege o stream"
