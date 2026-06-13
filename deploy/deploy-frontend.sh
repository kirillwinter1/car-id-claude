#!/bin/bash
# Деплой статического фронтенда Car-ID на prod.
#
# Синхронизирует каталог frontend/ на сервер по rsync (ssh) и перезагружает nginx.
# Скрипт параметризован через env (значения IP/пользователя — из приватного ops.inf,
# в git не коммитятся). По умолчанию работает интерактивно: показывает dry-run и
# спрашивает подтверждение перед реальной заливкой на prod.
#
# Использование:
#   CAR_ID_SSH_USER=<user> CAR_ID_SSH_HOST=<ip> ./deploy/deploy-frontend.sh
#   ./deploy/deploy-frontend.sh --yes        # без интерактивного подтверждения
#   ./deploy/deploy-frontend.sh --dry-run    # только показать, что изменится
#
# Опционально значения можно положить в deploy/frontend.env (он в .gitignore):
#   CAR_ID_SSH_USER=root
#   CAR_ID_SSH_HOST=79.174.94.70
#   CAR_ID_SSH_KEY=~/.ssh/car-id            # необязательно
#
# Env-переменные:
#   CAR_ID_SSH_USER     ssh-пользователь                       (required)
#   CAR_ID_SSH_HOST     ip/host prod-машины                    (required)
#   CAR_ID_SSH_PORT     ssh-порт                               (default: 22)
#   CAR_ID_SSH_KEY      путь до приватного ключа               (необязательно)
#   CAR_ID_FRONT_TARGET каталог на сервере                     (default: /var/www/car-id/car-id-front)
#   CAR_ID_FRONT_DELETE 1 — удалять на сервере файлы, которых нет локально (default: 0)
#   CAR_ID_SITE_URL     URL для проверки после деплоя          (default: https://car-id.ru)

set -euo pipefail

# --- разбор флагов -----------------------------------------------------------
ASSUME_YES=0
DRY_ONLY=0
for arg in "$@"; do
    case "$arg" in
        --yes|-y)     ASSUME_YES=1 ;;
        --dry-run|-n) DRY_ONLY=1 ;;
        -h|--help)    grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Неизвестный аргумент: $arg" >&2; exit 2 ;;
    esac
done

# --- пути --------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONT_DIR="$REPO_ROOT/frontend"

# опциональный локальный конфиг с реквизитами (вне git)
[ -f "$SCRIPT_DIR/frontend.env" ] && . "$SCRIPT_DIR/frontend.env"

# --- параметры ---------------------------------------------------------------
SSH_USER="${CAR_ID_SSH_USER:-}"
SSH_HOST="${CAR_ID_SSH_HOST:-}"
SSH_PORT="${CAR_ID_SSH_PORT:-22}"
SSH_KEY="${CAR_ID_SSH_KEY:-}"
TARGET="${CAR_ID_FRONT_TARGET:-/var/www/car-id/car-id-front}"
DO_DELETE="${CAR_ID_FRONT_DELETE:-0}"
SITE_URL="${CAR_ID_SITE_URL:-https://car-id.ru}"

# --- проверки ----------------------------------------------------------------
command -v rsync >/dev/null || { echo "Нет rsync в PATH" >&2; exit 1; }
[ -d "$FRONT_DIR" ] || { echo "Не найден каталог frontend: $FRONT_DIR" >&2; exit 1; }
if [ -z "$SSH_USER" ] || [ -z "$SSH_HOST" ]; then
    echo "Заданы не все реквизиты подключения." >&2
    echo "Укажи CAR_ID_SSH_USER и CAR_ID_SSH_HOST (env или deploy/frontend.env)." >&2
    exit 1
fi

# ssh-команда (с опциональным ключом и портом)
SSH_OPTS=(-p "$SSH_PORT")
[ -n "$SSH_KEY" ] && SSH_OPTS+=(-i "$SSH_KEY")
SSH_CMD="ssh ${SSH_OPTS[*]}"
REMOTE="${SSH_USER}@${SSH_HOST}"

# rsync: исключаем dev-мусор, не относящийся к боевому сайту
RSYNC_OPTS=(-az --human-readable --no-owner --no-group
            --exclude '.github' --exclude '.idea' --exclude '.git'
            --exclude '.DS_Store' --exclude '._*')
[ "$DO_DELETE" = "1" ] && RSYNC_OPTS+=(--delete)

echo "──────────────────────────────────────────────"
echo "  Источник : $FRONT_DIR/"
echo "  Цель     : ${REMOTE}:${TARGET}/"
echo "  ssh      : $SSH_CMD"
echo "  --delete : $([ "$DO_DELETE" = 1 ] && echo "ДА (стирает лишнее на сервере)" || echo "нет")"
echo "  Проверка : $SITE_URL"
echo "──────────────────────────────────────────────"

# --- dry-run -----------------------------------------------------------------
echo "▶ Пробный прогон (ничего не меняется):"
rsync "${RSYNC_OPTS[@]}" --dry-run -i -e "$SSH_CMD" "$FRONT_DIR/" "${REMOTE}:${TARGET}/"

if [ "$DRY_ONLY" = "1" ]; then
    echo "✓ Только dry-run (--dry-run). Выход."
    exit 0
fi

# --- подтверждение -----------------------------------------------------------
if [ "$ASSUME_YES" != "1" ]; then
    echo
    read -r -p "Залить эти изменения на PROD ($SSH_HOST) и перезагрузить nginx? [y/N] " ans
    case "$ans" in
        y|Y|yes|YES) ;;
        *) echo "Отменено."; exit 0 ;;
    esac
fi

# --- заливка -----------------------------------------------------------------
echo "▶ Заливаю файлы…"
rsync "${RSYNC_OPTS[@]}" -e "$SSH_CMD" "$FRONT_DIR/" "${REMOTE}:${TARGET}/"

# --- перезагрузка nginx (с проверкой конфига) --------------------------------
echo "▶ Проверяю конфиг и перезагружаю nginx…"
$SSH_CMD "$REMOTE" 'sudo nginx -t && sudo systemctl reload nginx'

# --- проверка ----------------------------------------------------------------
echo "▶ Проверяю сайт…"
code="$(curl -s -o /dev/null -w '%{http_code}' "$SITE_URL" || echo 000)"
if [ "$code" = "200" ]; then
    echo "✓ Готово: $SITE_URL отвечает 200."
else
    echo "⚠ Деплой выполнен, но $SITE_URL вернул HTTP $code — проверь вручную." >&2
    exit 1
fi
