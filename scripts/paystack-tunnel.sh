#!/usr/bin/env bash
# Exposes the locally running app to the internet with a temporary Cloudflare tunnel (no account needed),
# so Paystack in TEST mode can deliver real webhooks to your machine. Stop it with Ctrl+C.
#
# Usage: scripts/paystack-tunnel.sh        (PORT defaults to 1990)
set -euo pipefail

PORT="${PORT:-1990}"

if ! command -v cloudflared >/dev/null 2>&1; then
  echo "cloudflared is not installed. Install it with:  brew install cloudflared" >&2
  exit 1
fi

echo "Opening a tunnel to http://localhost:$PORT ..."
echo "Anyone with the URL can reach your local app while this runs; stop it when you're done."
cloudflared tunnel --no-autoupdate --url "http://localhost:$PORT" 2>&1 | while IFS= read -r line; do
  url="$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' <<<"$line" || true)"
  if [[ -n "$url" ]]; then
    echo
    echo "=================================================================="
    echo " Tunnel ready. In the Paystack dashboard (TEST mode):"
    echo "   Settings > API Keys & Webhooks > Test Webhook URL:"
    echo "   $url/webhook/paystack"
    echo "=================================================================="
    echo
  fi
done
