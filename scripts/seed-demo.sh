#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
SEED_KEY="${DEMO_SEED_KEY:-local-demo-seed-key}"

json() { python3 -c 'import json,sys; print(json.dumps(json.loads(sys.stdin.read())))'; }

echo "== register or login DEMO tenant =="
REG_CODE=$(curl -sS -o /tmp/bt-seed-reg.json -w '%{http_code}' -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
  -d '{"name":"Ayse","surname":"Yilmaz","email":"admin@bitalep.com","password":"Test1234!","companyName":"DEMO"}')
if [[ "$REG_CODE" == "201" ]]; then
  TOKEN=$(python3 -c 'import json; print(json.load(open("/tmp/bt-seed-reg.json"))["data"]["token"])')
else
  LOGIN=$(curl -sS -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
    -d '{"email":"admin@bitalep.com","password":"Test1234!"}')
  TOKEN=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["token"])' "$LOGIN")
fi
AUTH="Authorization: Bearer $TOKEN"

curl -sf -X PUT "$BASE/api/company" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"name":"DEMO"}' >/dev/null

create_user() {
  local email="$1" name="$2" surname="$3" role="$4" dept="$5"
  CODE=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/users" -H "$AUTH" -H "X-Demo-Seed: $SEED_KEY" -H 'Content-Type: application/json' \
    -d "{\"name\":\"$name\",\"surname\":\"$surname\",\"email\":\"$email\",\"role\":\"$role\",\"department\":\"$dept\",\"password\":\"Test1234!\"}")
  echo "  user $email ($CODE)"
}

echo "== personnel =="
create_user mehmet@bitalep.com Mehmet Kaya PERSONEL IT
create_user elif@bitalep.com Elif Demir PERSONEL HR
create_user can@bitalep.com Can Arslan PERSONEL FINANCE
create_user zeynep@bitalep.com Zeynep Koç PERSONEL SALES
create_user burak@bitalep.com Burak Şahin PERSONEL OPERATIONS
create_user selin@bitalep.com Selin Aydın PERSONEL MARKETING
create_user emre@bitalep.com Emre Yıldız PERSONEL OTHER
create_user deniz@bitalep.com Deniz Aksoy PERSONEL IT

USERS=$(curl -sf "$BASE/api/users?page=1&pageSize=50" -H "$AUTH")
MEHMET=$(python3 -c 'import json,sys
d=json.loads(sys.argv[1])["data"]
print(next(u["id"] for u in d if u["email"]=="mehmet@bitalep.com"))' "$USERS")

login_as() {
  sleep 1
  python3 -c 'import json,sys,urllib.request
req=urllib.request.Request(sys.argv[1]+"/api/auth/login", data=json.dumps({"email":sys.argv[2],"password":"Test1234!"}).encode(), headers={"Content-Type":"application/json"})
print(json.loads(urllib.request.urlopen(req).read())["data"]["token"])' "$BASE" "$1"
}

create_form() {
  local token="$1" title="$2" desc="$3" ftype="$4"
  curl -sf -X POST "$BASE/api/forms" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"title\":\"$title\",\"description\":\"$desc\",\"formType\":\"$ftype\"}"
}

echo "== forms =="
ADMIN_TOKEN="$TOKEN"
MEHMET_TOKEN=$(login_as mehmet@bitalep.com)
ELIF_TOKEN=$(login_as elif@bitalep.com)
CAN_TOKEN=$(login_as can@bitalep.com)

IDS=()
for spec in \
  "$MEHMET_TOKEN|Yıllık izin talebi|Temmuz ayında 5 gün yıllık izin.|LEAVE" \
  "$MEHMET_TOKEN|AWS eğitimi|Solutions Architect kursu katılımı.|TRAINING" \
  "$ELIF_TOKEN|Avans talebi|Acil nakit ihtiyacı için avans.|ADVANCE" \
  "$ELIF_TOKEN|Ofis malzemesi|Yazıcı toner ve A4 kağıt.|MATERIAL" \
  "$CAN_TOKEN|Q3 görev ataması|Finans kapanış checklist.|TASK" \
  "$MEHMET_TOKEN|Hastalık izni|İki günlük raporlu izin.|LEAVE" \
  "$CAN_TOKEN|Excel ileri eğitim|Power Query eğitimi.|TRAINING" \
  "$ELIF_TOKEN|Laptop talebi|Mevcut cihaz yetersiz.|MATERIAL"
do
  IFS='|' read -r tok title desc ftype <<<"$spec"
  RESP=$(create_form "$tok" "$title" "$desc" "$ftype")
  FID=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"]["id"])' "$RESP")
  IDS+=("$FID")
  echo "  form $title"
done

echo "== workflow =="
curl -sf -X PUT "$BASE/api/forms/${IDS[0]}/review" -H "$AUTH" >/dev/null
curl -sf -X PUT "$BASE/api/forms/${IDS[0]}/approve" -H "$AUTH" >/dev/null
curl -sf -X PUT "$BASE/api/forms/${IDS[1]}/review" -H "$AUTH" >/dev/null
curl -sf -X PUT "$BASE/api/forms/${IDS[2]}/review" -H "$AUTH" >/dev/null
curl -sf -X PUT "$BASE/api/forms/${IDS[2]}/reject" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"reason":"Bütçe bu dönem kapalı"}' >/dev/null
curl -sf -X PUT "$BASE/api/forms/${IDS[4]}/review" -H "$AUTH" >/dev/null

echo "== file =="
TMP=$(mktemp /tmp/bitalepXXXX.pdf)
printf '%%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%%%EOF\n' > "$TMP"
curl -sf -X POST "$BASE/api/files/upload" -H "Authorization: Bearer $MEHMET_TOKEN" \
  -F "file=@$TMP;type=application/pdf" -F "applicationId=${IDS[0]}" >/dev/null
rm -f "$TMP"

echo "== extra volume =="
for i in $(seq 1 12); do
  create_form "$MEHMET_TOKEN" "Ek talep $i" "Demo veri kaydı $i açıklaması yeterince uzun." TASK >/dev/null
done

echo "Seed complete. Demo: admin@bitalep.com / mehmet@bitalep.com  Test1234!"
