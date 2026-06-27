import { sentMessageNotice, readMessageNotice, STATUS_CALLING, STATUS_CALL_OWNER } from "./systemMessages.js";
import { openModal } from "./script.js";

const carEventsContainer = document.querySelector("#radio-btns");
const submitBtn = document.querySelector("#submit");
const modalContainer = document.querySelector(".modal-container");
const sectionSendMsg = document.querySelector("#send-msg");

// Бэкенд звонит владельцу через 30 сек, если уведомление всё ещё не прочитано
// (MessageService.asyncSend). Зеркалим этот момент в тексте статуса.
const CALL_ESCALATION_DELAY_MS = 30000;

let carEventList = [];
let timeoutId = 0;
let escalationTimerId = 0;
let callEnabled = false; // включён ли у владельца голосовой звонок (из status-эндпоинта, Тир 2)
const notificationId = window.location.pathname.slice(14, window.location.pathname.length);

buildEventList();

async function buildEventList() {
    const fetchUrl = window.location.origin + "/api/report/get_all_reasons";

    try {
        let response = await fetch(fetchUrl);
        if (response.ok) {
            carEventList = [...(await response.json())];

            carEventsContainer.innerHTML = carEventList.map((carEvent) => createCarEventRadioBtn(carEvent)).join("");
            submitBtn.removeAttribute("disabled", "");
            submitBtn.addEventListener("click", (event) => readMsg(event), false);
        } else {
            showError(response.status);
        }
    } catch (err) {
        showError(err);
    }
}

function createCarEventRadioBtn(carEvent) {
    return `<label class="custom-radio">${carEvent.description}<input type="radio" name="carEvent" id=${carEvent.id} /></label>`;
}

function readMsg(event) {
    event.preventDefault();
    const userChoice = document.querySelector("input:checked");
    if (userChoice === undefined || userChoice === null) return;
    carEventList.map((event) => (+event.id === +userChoice.id ? sendMsg(event.id) : false));
}

// Мгновенный отклик на клик, пока летит запрос: кнопка не должна выглядеть «мёртвой».
function setSubmitLoading(isLoading) {
    if (!submitBtn) return;
    if (isLoading) {
        submitBtn.dataset.label = submitBtn.textContent;
        submitBtn.textContent = "Отправляем…";
        submitBtn.setAttribute("disabled", "");
    } else {
        submitBtn.textContent = submitBtn.dataset.label || "Сообщить";
        submitBtn.removeAttribute("disabled");
    }
}

async function sendMsg(eventId) {
    const fetchUrl = window.location.origin + "/api/report/updateDraft";

    let body = {
        notification_id: notificationId,
        reason_id: eventId,
        text: "",
        status: "SEND"
    };

    setSubmitLoading(true);

    try {
        let response = await fetch(fetchUrl, {
            method: "PUT",
            headers: {
                accept: "application/json",
                "Content-Type": "application/json",
            },
            body: JSON.stringify(body),
        });

        if (response.ok) {
            let answer = await response.json();

            if (answer.notification_id) {
                showSentConfirmation();
                checkMsgStatus();
            } else if (answer.error_code === "SEND_TIMEOUT") {
                setSubmitLoading(false);
                showError(answer.error_message, "Чуть помедленнее");
            } else if (answer.error_code) {
                showError(answer.error_message, "Упс... Уже исправляем", closeErrorModal);
            } else {
                setSubmitLoading(false);
                showError(answer);
            }
        } else {
            setSubmitLoading(false);
            showError(response.status);
        }
    } catch (err) {
        setSubmitLoading(false);
        showError(err);
    }
}

// Показываем подтверждение СРАЗУ по 200 OK (не дожидаясь второго запроса статуса)
// и проматываем наверх — иначе подмена контента происходит вне поля зрения,
// пользователь думает, что «зависло», и закрывает страницу (см. отзыв на Ozon).
function showSentConfirmation() {
    sectionSendMsg.innerHTML = sentMessageNotice;
    window.scrollTo({ top: 0, behavior: "smooth" });

    escalationTimerId = setTimeout(() => {
        // Показываем «пробуем дозвониться» только если у владельца реально включён
        // звонок — иначе это обещание звонка, которого не будет (Тир 2).
        if (!callEnabled) return;
        const statusText = document.querySelector("#delivery-status .status-text");
        if (statusText) statusText.textContent = STATUS_CALLING;
    }, CALL_ESCALATION_DELAY_MS);
}

async function checkMsgStatus() {
    const fetchUrl = `${window.location.origin}/api/notification/${notificationId}/status`;

    try {
        let fetchAnswer = await fetch(fetchUrl);
        if (fetchAnswer.ok) {
            let msgStatus = await fetchAnswer.json();
            callEnabled = msgStatus.call_enabled === true;

            if (!["UNREAD", "READ"].includes(msgStatus.status)) {
                showError(msgStatus.status);
                return;
            }

            if (msgStatus.status === "UNREAD") {
                timeoutId = setTimeout(() => checkMsgStatus(), 5000);
            }
            if (msgStatus.status === "READ") {
                clearTimeout(timeoutId);
                clearTimeout(escalationTimerId);
                sectionSendMsg.innerHTML = readMessageNotice;
                window.scrollTo({ top: 0, behavior: "smooth" });
            }

            // BF5: владелец разрешил показ номера и прошёл порог → даём прямую кнопку звонка.
            // owner_phone приходит независимо от read/unread; рендерим после смены контента статуса.
            if (msgStatus.owner_phone) {
                showOwnerCallButton(msgStatus.owner_phone);
            }
        } else {
            showError(fetchAnswer.status);
        }
    } catch (err) {
        showError(err);
    }
}

// BF5: кнопка прямого звонка владельцу. Идемпотентна (один раз на страницу).
function showOwnerCallButton(phone) {
    if (document.getElementById("owner-call-link")) return;
    const a = document.createElement("a");
    a.id = "owner-call-link";
    a.href = "tel:" + phone;
    a.className = "owner-call-button";
    a.textContent = `${STATUS_CALL_OWNER}: ${phone}`;
    sectionSendMsg.appendChild(a);
}

function showError(err, title, cb) {
    const action = cb ? cb : closeModal;

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
        action
    );
}

function closeModal(event) {
    event.preventDefault();
    modalContainer.innerHTML = "";
    window.scrollTo(0, 0);
}

function closeErrorModal(event) {
    event.preventDefault();
    const baseUrl = "/";
    history.replaceState({ page: 1 }, `${window.location.pathname}`, baseUrl);
    window.location.pathname = baseUrl;
}
