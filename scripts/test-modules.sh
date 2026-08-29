#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
SUF=$(date +%s)
EMAIL="mod${SUF}@bitalep.com"
SEED_KEY="${DEMO_SEED_KEY:-local-demo-seed-key}"

REG=$(curl -sS -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Ada\",\"surname\":\"Admin\",\"email\":\"$EMAIL\",\"password\":\"Test1234!\",\"companyName\":\"Mod$SUF\"}")
test "$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["user"]["role"])' "$REG")" = "ADMIN"
TOKEN=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["token"])' "$REG")
AUTH="Authorization: Bearer $TOKEN"

echo "== company =="
COMP=$(curl -sS -o /tmp/bt-co.json -w '%{http_code}' "$BASE/api/company" -H "$AUTH")
test "$COMP" = "200"

echo "== create personnel =="
STAFF=$(curl -sS -X POST "$BASE/api/users" -H "$AUTH" -H "X-Demo-Seed: $SEED_KEY" -H 'Content-Type: application/json' \
  -d '{"name":"Mert","surname":"Personel","email":"mert'"$SUF"'@bitalep.com","role":"PERSONEL","department":"IT","password":"Test1234!"}')
test "$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["role"])' "$STAFF")" = "PERSONEL"
SID=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["id"])' "$STAFF")

STOKEN=$(python3 -c 'import json,sys,urllib.request
req=urllib.request.Request(sys.argv[1]+"/api/auth/login", data=json.dumps({"email":sys.argv[2],"password":"Test1234!"}).encode(), headers={"Content-Type":"application/json"})
print(json.loads(urllib.request.urlopen(req).read())["data"]["token"])' "$BASE" "mert${SUF}@bitalep.com")
SAUTH="Authorization: Bearer $STOKEN"

FORBIDDEN=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/users" -H "$SAUTH")
test "$FORBIDDEN" = "403"
COMP403=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/company" -H "$SAUTH")
test "$COMP403" = "403"

echo "== forms =="
FORM=$(curl -sS -o /tmp/bt-form.json -w '%{http_code}' -X POST "$BASE/api/forms" -H "$SAUTH" -H 'Content-Type: application/json' \
  -d '{"title":"Izin talebi","description":"Bes gunluk yillik izin talebi aciklamasi.","formType":"LEAVE"}')
test "$FORM" = "201"
FID=$(python3 -c 'import json; print(json.load(open("/tmp/bt-form.json"))["data"]["id"])')

LIST=$(curl -sS "$BASE/api/forms?page=1&pageSize=10" -H "$SAUTH")
python3 -c 'import json,sys; d=json.loads(sys.argv[1]); assert d["meta"]["totalItems"]>=1' "$LIST"

APR=$(curl -sS -o /tmp/bt-apr.json -w '%{http_code}' -X PUT "$BASE/api/forms/$FID/approve" -H "$SAUTH")
test "$APR" = "403"

echo "== workflow =="
REV=$(curl -sS -o /dev/null -w '%{http_code}' -X PUT "$BASE/api/forms/$FID/approve" -H "$AUTH")
test "$REV" = "409"
curl -sf -X PUT "$BASE/api/forms/$FID/review" -H "$AUTH" >/dev/null
curl -sf -X PUT "$BASE/api/forms/$FID/approve" -H "$AUTH" >/dev/null

echo "== files =="
TMP=$(mktemp /tmp/btXXXX.pdf)
printf '%%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%%%EOF\n' > "$TMP"
UP=$(curl -sS -o /tmp/bt-up.json -w '%{http_code}' -X POST "$BASE/api/files/upload" -H "$SAUTH" \
  -F "file=@$TMP;type=application/pdf" -F "applicationId=$FID")
test "$UP" = "201"
rm -f "$TMP"

echo "== dashboard + notifications =="
curl -sf "$BASE/api/dashboard" -H "$AUTH" >/dev/null
COUNT=$(curl -sS "$BASE/api/notifications/unread-count" -H "$SAUTH")
python3 -c 'import json,sys; assert json.loads(sys.argv[1])["data"]["count"]>=1' "$COUNT"
curl -sf -X PUT "$BASE/api/notifications/read-all" -H "$SAUTH" >/dev/null
COUNT0=$(curl -sS "$BASE/api/notifications/unread-count" -H "$SAUTH")
python3 -c 'import json,sys; assert json.loads(sys.argv[1])["data"]["count"]==0' "$COUNT0"

echo "== forgot password =="
FP=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/forgot-password" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\"}")
test "$FP" = "200"

echo "module tests passed"
