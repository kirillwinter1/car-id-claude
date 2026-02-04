export const sentMessageNotice = `
    <div class="modal">
        <div class="container">
            <h2>Cообщение отправлено</h2>
            <p>Владелец авто получит уведомление в приложение Car ID.</p>
            <p>Ожидаем ответа от владельца автомобиля.</p>
            <p>Не закрывайте браузер и разрешите отправку уведомлений.</p>
            <button id="close-btn" class="primary-btn">Понятно</button>
        </div>
    </div>
`;

export const waitingForReadingNotice = `
    <div class="notice">
        <h2>Сообщение доставлено</h2>
        <p>Ждём, когда прочитают.</p>
        <div class="img">
            <img src="/img/spinner.svg" width="75" height="75" alt="spinner" class="spinner"/>
        </div>
    </div>
`;

export const readMessageNotice = `
    <div class="notice">
        <h2>Сообщение прочитано</h2>
        <p>Ожидайте водителя.</p>
        <div class="img">
            <img src="/img/check-circle.svg" width="68" height="68" alt="check mark" />
        </div>
    </div>
`;
