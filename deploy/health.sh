#!/bin/bash
# Healthcheck Car-ID. При не-200 — алерт в Telegram через send_message.sh.
# Запускается systemd-таймером health.timer (каждые 5 секунд).

set -euo pipefail

HEALTH_URL="${CAR_ID_HEALTH_URL:-http://localhost:8081/actuator/health}"
SEND_MESSAGE_SCRIPT="${CAR_ID_SEND_MESSAGE_SCRIPT:-/var/www/car_id/send_message.sh}"

status_code=$(curl --write-out '%{http_code}' --silent --output /dev/null "$HEALTH_URL")

if [[ "$status_code" -ne 200 ]]; then
    "$SEND_MESSAGE_SCRIPT" "CarID status changed to $status_code"
fi
