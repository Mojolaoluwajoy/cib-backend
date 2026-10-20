#!/usr/bin/env bash
# Sends a correctly signed fake Paystack webhook to the locally running app, so deposits, fundings
# and payouts can be tested without Paystack or a tunnel. The signature uses PAYSTACK_SECRET_KEY from .env.
#
# Usage:
#   scripts/send-test-webhook.sh dva-deposit      <customer_code> <amount_naira> [reference]
#   scripts/send-test-webhook.sh charge-success   <transaction_reference> <amount_naira>
#   scripts/send-test-webhook.sh transfer-success <transaction_reference>
#   scripts/send-test-webhook.sh transfer-failed  <transaction_reference>
#
# Environment: BASE_URL (default http://localhost:1990), ENV_FILE (default .env)
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:1990}"
ENV_FILE="${ENV_FILE:-.env}"

usage() { sed -n '5,10p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }

secret="${PAYSTACK_SECRET_KEY:-}"
if [[ -z "$secret" && -f "$ENV_FILE" ]]; then
  secret="$(grep -E '^PAYSTACK_SECRET_KEY=' "$ENV_FILE" | tail -1 | cut -d= -f2-)"
fi
[[ -n "$secret" ]] || { echo "PAYSTACK_SECRET_KEY not set (export it or add it to $ENV_FILE)" >&2; exit 1; }

kobo() { awk -v n="$1" 'BEGIN { printf "%d", n * 100 + 0.5 }'; }

event="${1:-}"
case "$event" in
  dva-deposit)
    [[ $# -ge 3 ]] || usage
    reference="${4:-T_local_$(date +%s)}"
    payload="{\"event\":\"charge.success\",\"data\":{\"reference\":\"$reference\",\"amount\":$(kobo "$3"),\"status\":\"success\",\"channel\":\"dedicated_nuban\",\"customer\":{\"customer_code\":\"$2\"}}}"
    ;;
  charge-success)
    [[ $# -ge 3 ]] || usage
    payload="{\"event\":\"charge.success\",\"data\":{\"reference\":\"$2\",\"amount\":$(kobo "$3"),\"status\":\"success\",\"customer\":{\"customer_code\":\"CUS_local_test\"}}}"
    ;;
  transfer-success|transfer-failed)
    [[ $# -ge 2 ]] || usage
    payload="{\"event\":\"${event/-/.}\",\"data\":{\"reference\":\"$2\"}}"
    ;;
  *) usage ;;
esac

signature="$(printf '%s' "$payload" | openssl dgst -sha512 -hmac "$secret" | awk '{print $NF}')"

echo "POST $BASE_URL/webhook/paystack  ($event)"
curl -sS --max-time 30 -w '\nHTTP %{http_code}\n' -X POST "$BASE_URL/webhook/paystack" \
  -H 'Content-Type: application/json' \
  -H "x-paystack-signature: $signature" \
  -d "$payload"
