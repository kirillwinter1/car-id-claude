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
                createMsg();
                return;
            }

            if (response.error_message) {
                showError(response.error_message);
                return;
            }
        } else {
            showError(response.status);
        }
    } catch (err) {
        showError(err);
    }
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
