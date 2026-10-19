# Testing Paystack locally

Paystack talks to this app in two directions:

- **Outgoing** (app → Paystack): creating DVAs, starting card funding, sending payouts. These work locally
  as soon as `PAYSTACK_SECRET_KEY` / `PAYSTACK_PUBLIC_KEY` in `.env` hold your Paystack **test** keys.
- **Incoming** (Paystack → app): webhooks to `POST /webhook/paystack` that confirm payments, deposits and
  transfers. Paystack can't reach `localhost`, so pick one of the options below.

## Option 1: simulate webhooks (no internet needed)

`scripts/send-test-webhook.sh` sends a correctly signed webhook to the running app, using the
`PAYSTACK_SECRET_KEY` from `.env`. This is the quickest way to test the money flows.

```bash
# Money arriving in an organization's dedicated virtual account (credits its first NGN account)
scripts/send-test-webhook.sh dva-deposit CUS_xxxxxxxx 5000

# A card/bank funding started from POST /external/transaction/fund completed
# (use the transactionReference returned by that endpoint, and the same amount)
scripts/send-test-webhook.sh charge-success 3f2c9a1e-... 5000

# An approved payout succeeded or failed
scripts/send-test-webhook.sh transfer-success 7b1d04c2-...
scripts/send-test-webhook.sh transfer-failed  7b1d04c2-...
```

The organization's customer code (`CUS_...`) is stored on the organization when it is approved.

## Option 2: real Paystack webhooks through a tunnel

1. `brew install cloudflared` (one-time)
2. Start the app, then run `scripts/paystack-tunnel.sh`
3. Copy the printed URL into the Paystack dashboard, **test mode**:
   *Settings → API Keys & Webhooks → Test Webhook URL* → `https://<random>.trycloudflare.com/webhook/paystack`
4. Trigger a real test payment. Paystack's test cards are listed at
   https://paystack.com/docs/payments/test-payments

The URL changes every time the tunnel starts, so update the dashboard each time. Anyone with the URL can
reach your local app while the tunnel runs, so stop it (Ctrl+C) when you're done.

## Payouts

Turn off **Transfer OTP** in the Paystack dashboard (*Settings → Preferences*), otherwise transfers wait for
an OTP and never complete.
