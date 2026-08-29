# Host nginx snippets for vetapp-prod. Do not drop in until SUB-B18 fills ports.
# Install as /etc/nginx/sites-available/bitalep-* and symlink into sites-enabled.
# Certbot: certbot --nginx -d api.bitalep.com.tr -d panel.bitalep.com.tr -d bitalep.com.tr -d www.bitalep.com.tr

# api.bitalep.com.tr → 127.0.0.1:${API_HOST_PORT}
# panel.bitalep.com.tr → 127.0.0.1:${PANEL_HOST_PORT}
# bitalep.com.tr + www → 302 https://panel.bitalep.com.tr
