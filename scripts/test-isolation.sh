#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
SUF=$(date +%s)

register() {
  local email="$1" company="$2"
  curl -sS -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
    -d "{\"name\":\"A\",\"surname\":\"B\",\"email\":\"$email\",\"password\":\"Test1234!\",\"companyName\":\"$company\"}"
}

A=$(register "a${SUF}@ex.com" "TenantA")
B=$(register "b${SUF}@ex.com" "TenantB")
TA=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["token"])' "$A")
TB=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["token"])' "$B")

FORM=$(curl -sS -X POST "$BASE/api/forms" -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' \
  -d '{"title":"Secret form","description":"Should be invisible to other tenant.","formType":"LEAVE"}')
FID=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["id"])' "$FORM")

CROSS=$(curl -sS -o /tmp/bt-cross.json -w '%{http_code}' "$BASE/api/forms/$FID" -H "Authorization: Bearer $TB")
test "$CROSS" = "404"

CODE=$(python3 -c 'import json; print(json.load(open("/tmp/bt-cross.json"))["error"]["code"])')
test "$CODE" = "NOT_FOUND"

echo "isolation tests passed"
