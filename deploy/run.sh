#!/bin/bash
# Стартовый wrapper для Car-ID. Вызывается из car_id.service.

set -euo pipefail

JAVA_BIN="${CAR_ID_JAVA_BIN:-/usr/lib/jvm/jdk-21.0.3/bin/java}"
CONFIG_PATH="${CAR_ID_CONFIG_PATH:-/var/www/car_id/application-prod.yml}"
JAR_PATH="${CAR_ID_JAR_PATH:-/var/www/car_id/jar/car_id.jar}"
PID_FILE="${CAR_ID_PID_FILE:-/var/www/car_id/car_id.pid}"
SEND_MESSAGE_SCRIPT="${CAR_ID_SEND_MESSAGE_SCRIPT:-/var/www/car_id/send_message.sh}"

"$SEND_MESSAGE_SCRIPT" "start application" || true

"$JAVA_BIN" \
    -Dspring.config.location="$CONFIG_PATH" \
    -jar "$JAR_PATH" &

echo $! > "$PID_FILE"
