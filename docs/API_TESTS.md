# API contract and reproducible checks

Start the database and backend using the root README. The following sequence requires Bash, `curl`, `jq`, and OpenSSL. It creates two uniquely named test accounts and changes only their virtual portfolios. It never prints a password or bearer token. Run it on a development database; its test users and transaction history remain afterward.

The default API base is `http://localhost:8080/api`. Set `API_BASE` before the script if the backend uses another address. Disable shell tracing when handling tokens. Do not paste authentication responses into issue reports.

## Endpoints

| Method and path | Request | Access |
| --- | --- | --- |
| `POST /auth/register` | `{name,email,password}` | Public; returns token and user |
| `POST /auth/login` | `{email,password}` | Public; returns token and user |
| `GET /auth/me` | None | Authenticated caller's profile |
| `GET /wallet/{userId}` | None | Owner |
| `POST /wallet/{userId}/deposit` | `{amount}` | Owner; positive INR, at most 2 decimals |
| `GET /stocks` | None | Public |
| `GET /stocks/{id}` | None | Public |
| `GET /stocks/{id}/history` | None | Public; chronological samples |
| `POST /trade/buy` | `{userId,stockId,amount}` | Owner; ₹10–₹500, at most 2 decimals |
| `POST /trade/sell` | `{userId,stockId,quantity}` | Owner; positive quantity, at most 4 decimals |
| `GET /portfolio/{userId}` | None | Owner |
| `GET /transactions/{userId}?page=0&size=20` | None | Owner; zero-based pages |
| `GET /transactions/{userId}/export` | None | Owner; CSV download |
| `GET /leaderboard` | None | Public classroom ranking |

Protected requests use `Authorization: Bearer` followed by the token returned by registration or login. An authentication response has `token`, `userId`, `name`, and `email`. A transaction page has `content`, `page`, `size`, `totalElements`, and `totalPages`. Stock history uses the numeric stock ID, not its symbol.

## Copy-paste test sequence

```bash
#!/usr/bin/env bash
set -euo pipefail
set +x
for tool in curl jq openssl; do
  command -v "$tool" >/dev/null || { printf 'Missing command: %s\n' "$tool"; exit 1; }
done
API_BASE="${API_BASE:-http://localhost:8080/api}"
API_BASE="${API_BASE%/}"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"; unset TOKEN PASSWORD' EXIT
PASSWORD="$(openssl rand -hex 20)"
SUFFIX="$(date +%s)-$(openssl rand -hex 4)"
EMAIL_A="api-a-${SUFFIX}@example.test"
EMAIL_B="api-b-${SUFFIX}@example.test"
TOKEN=''

request() {
  local expected="$1" method="$2" route="$3" payload="${4:-}" auth="${5:-yes}"
  local -a args=(-sS --connect-timeout 5 --max-time 30 -X "$method"
    -H 'Content-Type: application/json' -o "$TEMP_DIR/response.json" -w '%{http_code}')
  if [[ "$auth" == yes && -n "$TOKEN" ]]; then
    args+=(-H "Authorization: Bearer $TOKEN")
  fi
  if [[ -n "$payload" ]]; then args+=(--data "$payload"); fi
  local status
  status="$(curl "${args[@]}" "$API_BASE$route")"
  if [[ "$status" != "$expected" ]]; then
    printf 'FAIL %s %s: expected %s, received %s\n' "$method" "$route" "$expected" "$status"
    # Authentication responses can contain tokens; do not print their raw bodies.
    jq '{status,error,message,path}' "$TEMP_DIR/response.json" 2>/dev/null || true
    exit 1
  fi
  if (( expected >= 400 )); then
    jq -e --argjson status "$expected" --arg path "/api${route%%\?*}" \
      '.timestamp and .status == $status and .error and .message and .path == $path' \
      "$TEMP_DIR/response.json" >/dev/null
  fi
  printf 'PASS %s %s → %s\n' "$method" "$route" "$status"
}

REGISTER_A="$(jq -nc --arg email "$EMAIL_A" --arg password "$PASSWORD" \
  '{name:"API Student A",email:$email,password:$password}')"
request 201 POST /auth/register "$REGISTER_A" no
USER_ID="$(jq -er '.userId' "$TEMP_DIR/response.json")"
TOKEN="$(jq -er '.token' "$TEMP_DIR/response.json")"

REGISTER_B="$(jq -nc --arg email "$EMAIL_B" --arg password "$PASSWORD" \
  '{name:"API Student B",email:$email,password:$password}')"
request 201 POST /auth/register "$REGISTER_B" no
OTHER_USER_ID="$(jq -er '.userId' "$TEMP_DIR/response.json")"

LOGIN="$(jq -nc --arg email "$EMAIL_A" --arg password "$PASSWORD" \
  '{email:$email,password:$password}')"
request 200 POST /auth/login "$LOGIN" no
TOKEN="$(jq -er '.token' "$TEMP_DIR/response.json")"
request 200 GET /auth/me
jq -e --argjson user "$USER_ID" --arg email "$EMAIL_A" \
  '.userId == $user and .email == $email and .name == "API Student A"' \
  "$TEMP_DIR/response.json" >/dev/null
request 200 GET "/wallet/$USER_ID"
jq -e '.balance == 1000' "$TEMP_DIR/response.json" >/dev/null
request 200 GET "/portfolio/$USER_ID"
jq -e '.holdings | length == 0' "$TEMP_DIR/response.json" >/dev/null

request 200 GET /stocks '' no
jq -e 'length >= 8' "$TEMP_DIR/response.json" >/dev/null
STOCK_ID="$(jq -er '.[0].id' "$TEMP_DIR/response.json")"
request 200 GET "/stocks/$STOCK_ID" '' no
request 200 GET "/stocks/$STOCK_ID/history" '' no
jq -e 'type == "array"' "$TEMP_DIR/response.json" >/dev/null
request 200 POST "/wallet/$USER_ID/deposit" '{"amount":100}'
jq -e '.balance == 1100' "$TEMP_DIR/response.json" >/dev/null

BUY="$(jq -nc --argjson user "$USER_ID" --argjson stock "$STOCK_ID" \
  '{userId:$user,stockId:$stock,amount:100}')"
request 200 POST /trade/buy "$BUY"
jq -e '.type == "BUY" and .quantity > 0 and .amount == 100 and .remainingBalance == 1000' \
  "$TEMP_DIR/response.json" >/dev/null

# Exhaust the remaining balance with two valid buys, then verify insufficient funds.
BUY_500="$(jq -c '.amount=500' <<<"$BUY")"
request 200 POST /trade/buy "$BUY_500"
request 200 POST /trade/buy "$BUY_500"
request 400 POST /trade/buy "$BUY"
request 200 GET "/wallet/$USER_ID"
jq -e '.balance == 0' "$TEMP_DIR/response.json" >/dev/null

# Partial sell retains the position; an excessive sell must leave it unchanged.
SELL="$(jq -nc --argjson user "$USER_ID" --argjson stock "$STOCK_ID" \
  '{userId:$user,stockId:$stock,quantity:0.0001}')"
request 200 POST /trade/sell "$SELL"
jq -e '.type == "SELL" and .quantity == 0.0001' "$TEMP_DIR/response.json" >/dev/null
request 400 POST /trade/sell "$(jq -c '.quantity=100000' <<<"$SELL")"
request 200 GET "/portfolio/$USER_ID"
REMAINING="$(jq -er --argjson stock "$STOCK_ID" \
  '.holdings[] | select(.stockId==$stock) | .quantity' "$TEMP_DIR/response.json")"
request 200 POST /trade/sell "$(jq -c --argjson quantity "$REMAINING" '.quantity=$quantity' <<<"$SELL")"
request 200 GET "/portfolio/$USER_ID"
jq -e '.holdings | length == 0' "$TEMP_DIR/response.json" >/dev/null

request 200 GET "/transactions/$USER_ID?page=0&size=2"
jq -e '.page==0 and .size==2 and (.content|length)==2 and .totalElements==6 and .totalPages==3' \
  "$TEMP_DIR/response.json" >/dev/null
request 200 GET "/transactions/$USER_ID/export"
[[ "$(head -n 1 "$TEMP_DIR/response.json")" == 'id,type,symbol,quantity,price,amount,created_at' ]]
[[ "$(wc -l < "$TEMP_DIR/response.json")" -eq 7 ]]
request 200 GET /leaderboard '' no
jq -e 'type == "array"' "$TEMP_DIR/response.json" >/dev/null

# Ownership, authentication, validation, missing-resource, and malformed-JSON checks.
request 401 GET "/wallet/$USER_ID" '' no
request 401 GET /auth/me '' no
request 403 GET "/wallet/$OTHER_USER_ID"
request 403 GET "/portfolio/$OTHER_USER_ID"
request 403 GET "/transactions/$OTHER_USER_ID"
request 403 GET "/transactions/$OTHER_USER_ID/export"
request 403 POST "/wallet/$OTHER_USER_ID/deposit" '{"amount":1}'
request 403 POST /trade/buy "$(jq -c --argjson user "$OTHER_USER_ID" '.userId=$user' <<<"$BUY")"
request 400 POST /trade/buy "$(jq -c '.amount=9.99' <<<"$BUY")"
request 400 POST /trade/buy "$(jq -c '.amount=500.01' <<<"$BUY")"
request 400 POST /trade/sell "$(jq -c '.quantity=0.12345' <<<"$SELL")"
request 400 POST "/wallet/$USER_ID/deposit" '{"amount":0}'
request 400 POST /trade/buy '{'
request 400 GET "/transactions/$USER_ID?page=-1&size=20"
request 404 GET /stocks/9223372036854775807 '' no
BAD_LOGIN="$(jq -nc --arg email "$EMAIL_A" '{email:$email,password:"definitely-wrong-password"}')"
request 401 POST /auth/login "$BAD_LOGIN" no

# Parallel deposits: every successful request must appear exactly once in balance.
# A genuine stale writer returns 409. A fast/serial run may validly have no conflicts.
request 200 GET "/wallet/$USER_ID"
BEFORE="$(jq -er '.balance' "$TEMP_DIR/response.json")"
pids=()
for i in {1..24}; do
  curl -sS --connect-timeout 5 --max-time 30 -X POST \
    -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
    --data '{"amount":1.00}' -o "$TEMP_DIR/parallel-$i.json" \
    -w '%{http_code}' "$API_BASE/wallet/$USER_ID/deposit" > "$TEMP_DIR/status-$i" &
  pids+=("$!")
done
for pid in "${pids[@]}"; do wait "$pid"; done
SUCCESS_COUNT=0
CONFLICT_COUNT=0
for i in {1..24}; do
  status="$(cat "$TEMP_DIR/status-$i")"
  case "$status" in
    200) SUCCESS_COUNT=$((SUCCESS_COUNT+1));;
    409) CONFLICT_COUNT=$((CONFLICT_COUNT+1));
      jq -e '.status==409 and .timestamp and .error and .message and .path' \
        "$TEMP_DIR/parallel-$i.json" >/dev/null;;
    *) printf 'Unexpected parallel status: %s\n' "$status"; exit 1;;
  esac
done
request 200 GET "/wallet/$USER_ID"
jq -e --argjson before "$BEFORE" --argjson count "$SUCCESS_COUNT" \
  '.balance == ($before+$count)' "$TEMP_DIR/response.json" >/dev/null
request 200 GET "/transactions/$USER_ID?page=0&size=100"
jq -e --argjson count "$SUCCESS_COUNT" '.totalElements == (6+$count)' \
  "$TEMP_DIR/response.json" >/dev/null
printf 'PASS concurrent deposits: %s commits, %s conflicts; balance and log agree.\n' \
  "$SUCCESS_COUNT" "$CONFLICT_COUNT"
printf 'API sequence completed for user IDs %s and %s.\n' "$USER_ID" "$OTHER_USER_ID"
```

## What these checks establish

The sequence verifies fresh account balances; registration, login, and the current profile; stock detail and history; deposits; buys; partial and full sells; portfolio calculation shape; paginated history and CSV export; leaderboard availability; rejected overspending and overselling; ownership; validation; invalid JSON; missing stocks; and error fields. Parallel deposits check the persisted balance against the exact count of committed operations and transaction rows.

A parallel HTTP run cannot guarantee a particular scheduling interleaving. It is valid for all requests to finish sequentially with 200 responses. The automated stale-version and database concurrency tests provide deterministic coverage for the conflict rule. Do not interpret a zero-conflict manual run as proof that optimistic locking is absent.

The live price scheduler can change a stock between requests, so sell proceeds and P&L are deliberately checked through rules rather than hard-coded future prices. The database tests control prices for exact arithmetic assertions. Initial rounding can create a small valuation difference immediately after a fractional buy.

## Browser acceptance checklist

1. Register, log out, and log in with email; refresh a protected page and confirm the session is restored.
2. Confirm the wallet starts at ₹1,000 and amounts use INR consistently.
3. Deposit virtual cash, buy ₹100 of a stock, view the fractional holding and history, then sell part and all of it.
4. Inspect the stock price chart; after a scheduler tick, confirm new history is available.
5. Verify insufficient funds, overselling, bad inputs, expired/invalid authentication, and network failures show useful messages.
6. Check transaction paging, portfolio empty states, leaderboard ranks, and the deposit-adjusted return.
7. At narrow mobile width, use navigation, trade forms, tables, and logout without horizontal page overflow.
8. Use keyboard navigation and inspect the browser console for uncaught errors. Do not include secrets or tokens in screenshots.

See the root README for automated build, unit, integration, and browser commands. A passing build alone does not establish that the database, API, or browser flow works.
