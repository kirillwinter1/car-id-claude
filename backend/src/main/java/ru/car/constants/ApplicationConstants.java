package ru.car.constants;

public class ApplicationConstants {
    public static final String CAR_ID_TITLE = "Car ID";
    public static final int SMS_NEXT_REQUEST_TIMEOUT_IN_SEC = 30;
    public static final long NOTIFICATION_LIVE_TIME_IN_MIN = 60L;
    public static final String DATETIME_FORMAT = "YYYY-MM-dd HH:mm:ss";

    public static final String HELLO_PUSH_MESSAGE = """
Добро пожаловать в сервис Car ID!
Здесь Вы можете зарегистрировать Qr через "Добавить метку".
Сканировать QR для отправки уведомления через "Сообщить о событии" или при помощи камеры телефона.
Также Вы можете просматривать "Уведомления" и настраивать каналы связи.

Для подключения уведомления из telegram:
 - зайдите в "Настроить уведомления"
 - нажмите "Подключить бота"
 - поделитесь контактом или напишите номер телефона
""";

    public static final String HELLO_TELEGRAM_MESSAGE = """
Рады приветствовать тебя в Car ID!
В сервисе Вы можете зарегистрировать Qr через "Добавить метку".
Сканировать QR для отправки уведомления через "Сообщить о событии" или при помощи камеры телефона.
Здесь Вы можете получать уведомления.
""";

    public static final String TELEGRAM_ALREADY_HAS_MESSAGE = """
К этому номеру телефона уже привязан аккаунт телеграм.
Свяжитесь с поддержкой и опишите свою проблему
""";

    public static final String CREATE_TEMPORARY_QR = """
Для Вас создан временный QR %s.
Пройдите по ссылке и отправьте уведомление
%s/qr/%s
""";

    public static final String USER_HAS_NO_QR = """
На текущий момент у Вас нет QR.
Вы можете получить временный QR и проверить его работоспособность либо приобрести постоянный по ссылке
https://ozon.ru/t/gXqGlgj
""";

}
