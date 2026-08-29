#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"

echo "== health =="
curl -sf "$BASE/actuator/health" | grep -q UP

echo "== register + login =="
SUF=$(date +%s)
EMAIL="t${SUF}@bitalep.com"
REG=$(curl -sS -o /tmp/bt-reg.json -w '%{http_code}' -X POST "$BASE/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Test\",\"surname\":\"User\",\"email\":\"$EMAIL\",\"password\":\"Test1234!\",\"companyName\":\"T$SUF\"}")
test "$REG" = "201"
TOKEN=$(python3 -c 'import json; print(json.load(open("/tmp/bt-reg.json"))["data"]["token"])')
REFRESH=$(python3 -c 'import json; print(json.load(open("/tmp/bt-reg.json"))["data"]["refreshToken"])')
test -n "$TOKEN"
test -n "$REFRESH"

LOGIN=$(curl -sS -o /tmp/bt-login.json -w '%{http_code}' -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Test1234!\"}")
test "$LOGIN" = "200"

BAD=$(curl -sS -o /tmp/bt-bad.json -w '%{http_code}' -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrong-pass\"}")
test "$BAD" = "401"

echo "== refresh rotation =="
REF=$(curl -sS -o /tmp/bt-ref.json -w '%{http_code}' -X POST "$BASE/api/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}")
test "$REF" = "200"
NEW_REFRESH=$(python3 -c 'import json; print(json.load(open("/tmp/bt-ref.json"))["data"]["refreshToken"])')
REUSE=$(curl -sS -o /tmp/bt-reuse.json -w '%{http_code}' -X POST "$BASE/api/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}")
test "$REUSE" = "401"

echo "== me + logout =="
ME=$(curl -sS -o /tmp/bt-me.json -w '%{http_code}' "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN")
test "$ME" = "200"
OUT=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$NEW_REFRESH\"}")
test "$OUT" = "204"

echo "== fake bearer =="
FAKE=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/forms" -H 'Authorization: Bearer not-a-jwt')
test "$FAKE" = "401"

echo "auth tests passed"
