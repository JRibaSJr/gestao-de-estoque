#!/usr/bin/env bash
# Testes de fumaça para o backend (executar na sua máquina)
# Uso: BASE_URL=http://localhost:8080 ./scripts/smoke-backend.sh
# Se BASE_URL não for informado, usa http://localhost:8080

set -euo pipefail
BASE_URL=${BASE_URL:-http://localhost:8080}
API="$BASE_URL/api"

say() { echo -e "\n==== $1 ===="; }

say "GET /inventory (lista completa)"
curl -s -S -X GET "$API/inventory" -H 'Accept: application/json' | head -c 500 && echo "\n..."

say "GET /inventory/low-stock?threshold=10"
curl -s -S -X GET "$API/inventory/low-stock?threshold=10" -H 'Accept: application/json' | head -c 500 && echo "\n..."

say "POST /inventory/stock-in (adiciona 5 unidades)"
curl -s -S -X POST "$API/inventory/stock-in" \
  -H 'Content-Type: application/json' \
  -d '{"storeId":1,"productId":1,"quantity":5,"referenceId":"PO-LOCAL-001","notes":"smoke test in"}'

say "POST /inventory/stock-out (remove 2 unidades)"
curl -s -S -X POST "$API/inventory/stock-out" \
  -H 'Content-Type: application/json' \
  -d '{"storeId":1,"productId":1,"quantity":2,"referenceId":"SALE-LOCAL-001","notes":"smoke test out"}'

say "POST /inventory/transfer (transferir 1 unidade da loja 1 para 2)"
curl -s -S -X POST "$API/inventory/transfer" \
  -H 'Content-Type: application/json' \
  -d '{"fromStoreId":1,"toStoreId":2,"productId":1,"quantity":1,"notes":"smoke transfer"}'

say "OK - Testes de fumaça concluídos"