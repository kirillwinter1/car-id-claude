import { waitingForReadingNotice, readMessageNotice } from "./systemMessages.js";
import { openModal } from "./script.js";

const carEventsContainer = document.querySelector("#radio-btns");
const submitBtn = document.querySelector("#submit");
const modalContainer = document.querySelector(".modal-container");
const sectionSendMsg = document.querySelector("#send-msg");

let carEventList = [];
let timeoutId = 0;
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

async function sendMsg(eventId) {
    const fetchUrl = window.location.origin + "/api/report/updateDraft";

    let body = {
        notification_id: notificationId,
        reason_id: eventId,
        text: "",
        status: "SEND"
    };

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
                checkMsgStatus();
            } else if (answer.error_code === "SEND_TIMEOUT") {
                showError(answer.error_message, "Чуть помедленнее");
            } else if (answer.error_code) {
                showError(answer.error_message, "Упс... Уже исправляем", closeErrorModal);
            } else showError(answer);
        } else {
            showError(response.status);
        }
    } catch (err) {
        showError(err);
    }
}

async function checkMsgStatus() {
    const fetchUrl = `${window.location.origin}/api/notification/${notificationId}/status`;

    try {
        let fetchAnswer = await fetch(fetchUrl);
        if (fetchAnswer.ok) {
            let msgStatus = await fetchAnswer.json();

            if (!["UNREAD", "READ"].includes(msgStatus.status)) showError(msgStatus.status);

            if (msgStatus.status === "UNREAD") {
                sectionSendMsg.innerHTML = waitingForReadingNotice;
                timeoutId = setTimeout(() => checkMsgStatus(), 5000);
            }
            if (msgStatus.status === "READ") {
                clearTimeout(timeoutId);
                sectionSendMsg.innerHTML = readMessageNotice;
            }
        } else {
            showError(response.status);
        }
    } catch (err) {
        showError(err);
    }
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
