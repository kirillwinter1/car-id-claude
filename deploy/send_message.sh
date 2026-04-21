#!/bin/bash
# Отправка сообщения в админский Telegram-канал (алерты healthcheck).
# Токен и chat ID — через env vars (см. deploy/README.md).

set -euo pipefail

TOKEN="${TELEGRAM_ALERT_BOT_TOKEN:?TELEGRAM_ALERT_BOT_TOKEN required}"
CHAT_ID="${TELEGRAM_ALERT_CHAT_ID:?TELEGRAM_ALERT_CHAT_ID required}"
MESSAGE="${1:-}"

if [ -z "$MESSAGE" ]; then
    echo "Usage: $0 <message>" >&2
    exit 1
fi

curl -s -X POST "https://api.telegram.org/bot${TOKEN}/sendMessage" \
    -d chat_id="${CHAT_ID}" \
    -d text="${MESSAGE}"
