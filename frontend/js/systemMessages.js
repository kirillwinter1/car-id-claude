// Тексты статуса ожидания. Меняем по таймеру, повторяя реальный сценарий бэкенда:
// push/Telegram уходят сразу, а на ~30-й секунде (если ещё не прочитано и у владельца
// включены звонки) MessageService.asyncSend делает реальный голосовой звонок.
export const STATUS_WAITING = "Ждём, пока владелец откроет сообщение";
export const STATUS_CALLING = "Владелец пока не открыл — пробуем дозвониться до него";

// Экран сразу после успешной отправки (updateDraft → 200 OK).
// Доминирующий сигнал — галочка «отправлено», а не крутящийся спиннер:
// спиннер читается как «грузится / зависло» (см. отзыв на Ozon).
// Строка #delivery-status обновляется по ходу ожидания (см. sendMsg.js).
export const sentMessageNotice = `
    <div class="notice">
        <div class="img">
            <img src="/img/check-circle.svg" width="68" height="68" alt="отправлено" />
        </div>
        <h2>Сообщение отправлено владельцу</h2>
        <p>Уведомление пришло ему в приложение Car ID.</p>
        <p class="delivery-status" id="delivery-status">
            <span class="dots" aria-hidden="true"><span></span><span></span><span></span></span>
            <span class="status-text">${STATUS_WAITING}</span>
        </p>
    </div>
`;

// Владелец прочитал уведомление.
export const readMessageNotice = `
    <div class="notice">
        <div class="img">
            <img src="/img/check-circle.svg" width="68" height="68" alt="прочитано" />
        </div>
        <h2>Сообщение прочитано</h2>
        <p>Ожидайте водителя.</p>
    </div>
`;
