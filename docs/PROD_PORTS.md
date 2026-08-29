# Production port / path notes (SUB-B18, 2026-08-29)

Host: `vetapp-prod` / `vetapp-test-server` / `46.62.248.74`

## Chosen (unused)

| Service | Bind |
|---------|------|
| API | `127.0.0.1:18080` → container 8080 |
| Panel | `127.0.0.1:18081` → container 80 |
| Postgres | compose network only (no host publish) |

18xxx was unused. Host `8080` is VetApp frontend — do not bind BiTalep there. Host `5432` is unused publicly but sibling DBs use `127.0.0.1:55432` / `55433` / `5433`.

## Layout

Deploy parent (same as VetApp/Zerafet): `/root/bitalep/`

```
/root/bitalep/BiTalep-backend
/root/bitalep/BiTalep-frontend
```

Nginx: **new** files only in `sites-available` + symlink. Existing `vetapp` / `mobilyahub` / `zerafet-api` untouched.

TLS: new certbot names `bitalep.com.tr`, `www.bitalep.com.tr`, `panel.bitalep.com.tr`, `api.bitalep.com.tr`. Existing live certs (`vetapp.com.tr`, `mobilyahub.com.tr`, …) untouched.
