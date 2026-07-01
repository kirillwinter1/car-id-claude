const modalContainer = document.querySelector(".modal-container");
const qrId = window.location.host.includes("github.io")
    ? window.location.pathname.slice(11, 47)
    : window.location.pathname.slice(4, 40);

const baseUrl = window.location.host.includes("github.io") ? "car-id/" : "/";

checkQr();

async function checkQr() {
    if (window.location.pathname.includes("notification")) return;

    const fetchUrl = `${window.location.origin}/api/qr/${qrId}`;

    try {
        let response = await fetch(fetchUrl);

        if (response.ok) {
            response = await response.json();

            if (response.status === "ACTIVE" || response.status === "TEMPORARY") {
                renderActions(response.owner_contacts);
                return;
            }

            if (response.error_message) {
                showError(response.error_message);
                return;
            }

            // Метка существует, но отправить по ней уведомление нельзя (NEW / DELETED).
            // Без этого пользователь видел бы дефолтную страницу «О сервисе» без объяснений.
            showQrUnavailable(response.status);
            return;
        } else {
            showError(response.status);
        }
    } catch (err) {
        showError(err);
    }
}

// BF6: развилка действий после скана — сообщить о событии / написать в мессенджер / позвонить.
// «Сообщить о событии» ведёт в текущий флоу (createMsg → выбор причины). Остальное — прямые ссылки.
function renderActions(contacts) {
    const box = document.getElementById("qr-actions");
    if (!box) { createMsg(); return; }

    const buttons = [actionButton("Сообщить о событии", null, "primary", createMsg)];
    if (contacts) {
        if (contacts.telegram) buttons.push(actionButton("Написать в Telegram", contacts.telegram));
        if (contacts.vk) buttons.push(actionButton("Написать в VK", contacts.vk));
        if (contacts.max) buttons.push(actionButton("Написать в MAX", contacts.max));
        if (contacts.phone) buttons.push(actionButton("Позвонить", "tel:" + contacts.phone));
    }
    box.replaceChildren(...buttons);
    box.hidden = false;
}

function actionButton(text, href, variant, onClick) {
    const el = href ? document.createElement("a") : document.createElement("button");
    el.className = "qr-action-btn" + (variant === "primary" ? " qr-action-primary" : "");
    el.textContent = text;
    if (href) {
        el.href = href;
        if (href.startsWith("http")) {
            el.target = "_blank";
            el.rel = "noopener noreferrer";
        }
    }
    if (onClick) el.addEventListener("click", (e) => { e.preventDefault(); onClick(); });
    return el;
}

async function createMsg() {
    try {
        const fetchUrl = window.location.origin + "/api/report/createDraft";

        let body = {
            qr_id: qrId,
            reason_id: 1
        };

        let response = await fetch(fetchUrl, {
            method: "POST",
            headers: {
                accept: "application/json",
                "Content-Type": "application/json",
            },
            body: JSON.stringify(body),
        });

        if (response.ok) {
            response = await response.json();

            if (![undefined, null, ""].includes(response.notification_id)) {
                let notificationId = "";

                // if (window.location.host.includes("car-id.ru"))
                notificationId = response.notification_id;

                // if (!window.location.host.includes("car-id.ru")) {
                //     notificationId = "guid";
                //     sessionStorage.setItem("notificationId", response.notification_id);
                // }

                history.replaceState({ page: 1 }, `${window.location.hostname}`, `/notification/${notificationId}`);
                window.location.pathname = `${baseUrl}notification/${notificationId}`;
                return;
            }
            showError(response.status);
        }

        if (response.error_message) {
            showError(response.error_message);
            return;
        }
    } catch (err) {
        showError(err);
    }
}

// Метка непригодна для отправки — показываем понятный статус вместо немой «О сервисе».
function showQrUnavailable(status) {
    const messages = {
        NEW: {
            title: "Метка ещё не активирована",
            text: "Эта метка пока не привязана к владельцу. Если метка ваша — активируйте её в приложении Car ID.",
        },
        DELETED: {
            title: "Метка отключена",
            text: "По этой метке больше нельзя отправить уведомление — владелец её отвязал.",
        },
    };
    const m = messages[status] || {
        title: "Метка недоступна",
        text: "По этой метке сейчас нельзя отправить уведомление.",
    };
    const about = document.querySelector("#about");
    if (about) {
        about.innerHTML = `<h2>${m.title}</h2><p>${m.text}</p>`;
    }
}

function showError(err, title) {
    console.log(err);
    openModal(
        `
        <div class="modal">
            <div class="container">
                <h2>${title ? title : "Сообщение об ошибке"}</h2>
                <p>${err === undefined ? "<p>Попробуйте обновить страницу и отправить завпрос позднее.</p>" : err}</p>
                <button id="close-btn" class="primary-btn">Понятно</button>
            </div>
        </div>
    `,
        closeErrorModal
    );
}

export function openModal(msgHtml, cb) {
    modalContainer.innerHTML = msgHtml;
    const closeBtn = document.querySelector("#close-btn");
    closeBtn.addEventListener("click", (event) => cb(event));
}

function closeErrorModal(event) {
    event.preventDefault();
    history.replaceState({ page: 1 }, `${window.location.pathname}`, baseUrl);
    window.location.pathname = baseUrl;
}
