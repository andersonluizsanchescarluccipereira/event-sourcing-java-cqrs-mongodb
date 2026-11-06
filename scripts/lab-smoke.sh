#!/usr/bin/env bash
set -euo pipefail
API="${API_URL:-http://localhost:8080}"
for cmd in curl jq python3; do command -v "$cmd" >/dev/null || { echo "Instale: $cmd"; exit 2; }; done
new_uuid() { python3 -c 'import uuid; print(uuid.uuid4())'; }
A="$(new_uuid)"; B="$(new_uuid)"; T="$(new_uuid)"
echo "[LAB] Origem=$A Destino=$B TransferId=$T"

post() {
  local endpoint="$1" json="$2"
  curl --fail-with-body -sS -H 'Content-Type: application/json' -X POST "$API$endpoint" -d "$json"
}
wait_balance() {
  local id="$1" expected="$2" tries=0
  until curl -fsS "$API/api/accounts/$id" 2>/dev/null |
      jq -e --argjson expected "$expected" '(.balance | tonumber) == $expected' >/dev/null 2>&1; do
    tries=$((tries+1))
    if (( tries > 30 )); then echo "ERRO: projecao nao atingiu saldo $expected para $id"; exit 1; fi
    sleep 1
  done
}

post /api/accounts "{\"accountId\":\"$A\",\"owner\":\"Ana\"}" | jq .
post /api/accounts "{\"accountId\":\"$B\",\"owner\":\"Beto\"}" | jq .
post "/api/accounts/$A/deposits" '{"amount":100.00,"reference":"PIX"}' | jq .
post "/api/accounts/$A/withdrawals" '{"amount":20.00,"reference":"ATM"}' | jq .
wait_balance "$A" 80

# Deliberately fail between the two independent stream appends.
JSON="{\"transferId\":\"$T\",\"from\":\"$A\",\"to\":\"$B\",\"amount\":30.00}"
STATUS="$(curl -sS -o /tmp/es-lab-failure.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -H 'X-Lab-Fail-After-Debit: true' -X POST "$API/api/transfers" -d "$JSON")"
if [[ "$STATUS" != "503" ]]; then echo "Esperado HTTP 503; recebido $STATUS"; exit 1; fi
wait_balance "$A" 50
wait_balance "$B" 0

# Retry with the SAME transferId; no double debit.
post /api/transfers "$JSON" | jq .
post /api/transfers "$JSON" | jq .
wait_balance "$A" 50
wait_balance "$B" 30

# Replay rebuilds the read model without changing the event stream.
post "/api/accounts/$A/replay" '{}' | jq .
wait_balance "$A" 50
EVENT_COUNT="$(curl -fsS "$API/api/accounts/$A/events" | jq 'length')"
[[ "$EVENT_COUNT" == "4" ]] || { echo "Esperado 4 eventos; encontrado $EVENT_COUNT"; exit 1; }
echo "[PASS] Event Sourcing + CQRS + consistencia eventual + saga retry + replay"
