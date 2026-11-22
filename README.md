# Русский

# Что это?

microtg - это проект, реализующий возможность реализации Telegram-клиентов на кнопочных телефонах и смартфонах.
Сам по себе, microtg - часть проекта "сам себе экосистема", где я сам пишу необходимые мне приложения для своих телефонов!

Концепция работы простая - сервер работает с TDLib, слушает события и посылает команды библиотеке, предоставляя клиентскому приложению REST-like API для работы с сообщениями.
При этом сервер выдаёт данные в виде простых пар ключ-значение, которые легко и просто парсить даже кнопочных телефонах начала 2000х годов!

Таким образом, клиентское приложение просто реализует фронтэнд для чатов с сервера, без необходимости реализации MTProto или нюансов сети.

# Безопасность

На данный момент, клиент-серверное сообщение происходит посредством plain/text посылок. Никакого шифрования пока нет, поэтому воздержитесь от использования microtg в открытых сетях!
В качестве сессии используется md5 хэш из номера телефона, кода авторизации и псевдослучайного числа (криптографически небезопасного). Это сделано для того, чтобы сервер нельзя было найти сканером портов и для реализации какой-никакой авторизации, дабы не гонять plain/text пароль по сети :)

# Как запустить?

Вам нужен сервер с белым IP (подойдет роутер, малинка, VDS за 180 рублей и т.п), бинарный дистрибутив TDLib (tdjson) и .NET Core.
Запуск:

dotnet tdsrv.dll

Программа запросит номер телефона и код доступа к Telegram. Вводим их, программа сгенерирует ключ для авторизации из приложений.
В клиентском приложении вводим IP нашего сервера с портом (сейчас 13377) и ключ авторизации. Готово!




# R1K1.SYS Android client (development)

## Этап 2: новое оформление (красно-фиолетовый стиль)

- Вход: после номера открывается отдельный экран «Введите код» (5 ячеек, автоотправка, повтор через 60 с, «Изменить номер»).
- Нижняя панель с нарисованными иконками; исправлена подсветка активной вкладки (раньше всегда горел «Chats»).
- Профиль: шапка с аватаркой, галочка, R1K1 ID (поле `r1k1_id`, пока его нет — берётся `tg_id`), подпись типа номера (`number_type`; без него временно по префиксу), карточки Bytes/Coins.
- «Деньги»: карточки Bytes и Coins со свайпом, кнопка «Отправить» (`POST /transfer` с `request_id`), вкладки «Транзакции» (`GET /transactions`) и «Коллекции» (`GET /collections`).
- Подарки: сетка «Купить подарок», обычные подарки 15/25/50/100 Bytes. Список берётся из `GET /gifts`; пока его нет, показывается список по умолчанию только для просмотра. Цены и списание подтверждает сервер (клиент отправляет только `gift_id` и получателя).
- Магазин: карточки вместо кнопок с GET-запросами (`/shop`, `/usernames/shop`, покупка ника `/usernames/buy`). Покупка предметов `/shop` подключится вместе с сервером.
- Настройки: сервер, активные сеансы (`GET /sessions`), подключение/отключение TDSrv, очистка кэша, выход. Конфиденциальность, уведомления, внешний вид и язык помечены «Скоро».
- Не затронуто в этой сборке: список чатов и экран переписки (старый вид), эмодзи/реакции, боты, каналы, роли.
- Сборка APK и запуск не проверялись (нет Android SDK): проверены XML и синтаксис Java.

## Этап 1: вход и профиль (изменения)

- Вход по номеру и коду: `POST /auth/request` -> код приходит в Telegram-бота -> `POST /auth/verify` -> токен сессии. Токен отправляется заголовком `Authorization: Bearer ...` (не в URL).
- Профиль берётся только с сервера: `GET /me` (имя, ID, телефон, Bytes/Coins, NFT, галочка `is_verified`). Ничего не выдумывается на клиенте, Premium показывается только если сервер вернул `premium: true`.
- Экран входа в красно-фиолетовом стиле на чёрном: градиентный логотип, карточка с полями, плавное появление поля кода.
- Выход из аккаунта (вкладка Profile). Если сервер вернул 401, клиент сам выходит и просит войти заново.
- Убран ключ по умолчанию `r1k1_super_secret_2026` и адрес `127.0.0.1:8000` из экрана входа. Если этот ключ совпадал с `API_KEY` сервера, замените его на сервере.
- Чаты работают через старый TDSrv: адрес и ключ задаются отдельно на вкладке Chats («Подключить TDSrv»). Старые сохранённые значения переносятся автоматически, но войти в backend нужно заново.
- Манифест: добавлен `android:exported` (без него сборка с targetSdk 33 падает), `allowBackup="false"`.

### Что нужно от сервера
`/auth/request`, `/auth/verify` и `/me` должны работать БЕЗ `x-api-key`: клиент не хранит секретных ключей. Пока сервер требует этот ключ, вход не заработает.

### Ограничения
- Токен хранится в SharedPreferences (приватное хранилище приложения, без шифрования).
- Без шифрования http допустим только для тестов; для реальной сети нужен https.
- Сборка APK здесь не выполнялась (нет Android SDK и сети). Проверены: XML парсится, Java-синтаксис разбирается; полная компиляция и запуск НЕ проверены.

---

## Предыдущее описание

The Android client now has a black/neon-red R1K1.SYS shell with Profile, Shop, Gifts and Chats tabs. The existing chat list and message screen remain in the project. The login form defaults to `127.0.0.1:8000` and `r1k1_super_secret_2026`, and checks `GET /CheckCredentials?auth_key=...` for a trimmed `OK` response.

The Profile/Shop/Gifts actions call the custom HTTP API through `ClientManager`. These screens are a client-side integration foundation, not a claim that every feature is complete: the server must implement the relevant endpoints and payload contracts. The profile refresh asks for a user ID; the client does not invent one. Giveaway participation currently needs the backend's expected giveaway identifier/payload to be finalized.

The existing `TDSrv` directory has not been modified. It is retained for the original chat transport. The Node.js server is intentionally not included. Use HTTPS on any network beyond a trusted development environment; the default localhost address is for local development only.

Build workflow: `.github/workflows/build.yml` runs `assembleDebug` on GitHub Actions. A local APK build was not verified in this environment because Gradle distribution download was blocked by unavailable network access.
