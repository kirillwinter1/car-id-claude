# car-id

## сервисы контура

### /etc/systemd/system/car_id.service

запуск приложения
```bash
/bin/bash /var/www/car_id/run.sh
```

### /etc/systemd/system/deploy.service

сервис был придуман, чтобы запускать сервис из другого места
осталось от гитлаба
его в конечном итоге будет дергать ci/cd.
пока хз, что он будет делать...
либо полностью проект собирать и копировать его в исполняемую папку
либо только перезагружать сервис

### /etc/systemd/system/backup.service
```
запускается скрипт каждый день в 3:00 по таймеру
/usr/lib/systemd/system/backup.timer
из
/var/www/car_id/backup.sh
в
/var/www/car_id/backup

скрипт снимает дамб в папку с дамбами
```

### /etc/systemd/system/health.service
```
запускается скрипт каждые 5 секунд по таймеру
/usr/lib/systemd/system/health.timer
из
/var/www/car_id/health.sh

скрипт проверяет работу приложения и спамит в канал телеги
```


## написать инструкцию для генерации qr

1) генерация qr в svg

### Ищем в проекте класс
```
ru.car.util.SvgUtils
```

### ищем функцию main
### В ней проверяем название папки, куда будум создавать qr (не стоит создавать их в папку прошлой партии)
```java
String dir = "stickersXX";
```

### Корректируем шаблоны, количества и номер батча для бд
```java
List<Triple<BatchTemplates, Integer, Integer>> templates = new ArrayList<>();
templates.add(Triple.of(BatchTemplates.PT_WHITE_1, 1000, 22));
templates.add(Triple.of(BatchTemplates.PT_BLACK_1, 2000, 23));
```

### запускаем функцию. Это создаст подпапки с svg файлами qr

2) далее переводим svg в pdf

### Ищем в проекте класс
```
ru.car.util.svg.PdfConverter
```

### ищем функцию main

### В ней проверяем название папки, где лежат все svg файлы
```java
String root = "/Users/work/IdeaProjects/carId/stickersXX";
```

### запускаем
### По итогу в папке появляются подпапки с pdf файлами и также из каждой подпапки все файлы склеиваются в отдельный файл

## написать инструкцию для деплоя фронта и бека

1) бек

### собираем проект и копируем на стенд
```bash
gradle bootJar
scp ./build/libs/carId.jar root@79.174.94.70:/var/www/car_id/jar
пароль
```

### подключаемся к контуру
```bash
ssh root@79.174.94.70
пароль
```

### перезагружаем сервис делоя
```bash
systemctl restart deploy.service
```

2) фронт

### заходим в проект и собираем его в архив
```bash
cd ..
rm -rf archive.tar.gz
tar -czf archive.tar.gz car-id-front
```

### копируем архив на стенд
```bash
scp ./archive.tar.gz root@79.174.94.70:/var/www/car_id/
пароль
```

### заходим на стенд
```bash
ssh root@79.174.94.70
пароль
```

```bash
cd /var/www/car_id
rm -rf car-id-front
tar -xvf archive.tar.gz
```

### перезагружаем nginx
```bash
sudo systemctl restart nginx
```

## раз в полгода или год надо переделывать сертификаты 

### скачиваем у поставщика certificate.key

### certificate.pem собираем из certificate.crt и certificate_ca.crt
```bash
# он состоит из 3 частей:
# из certificate.crt 41 строчка
-----BEGIN CERTIFICATE-----
MIIHhTCCBm2gAw...
A/GtpGHCo6xN
-----END CERTIFICATE-----

# 1 из certificate_ca.crt на 26 строчек
-----BEGIN CERTIFICATE-----
MIIEsDCCA5igAw...
D/fayQ==
-----END CERTIFICATE-----

# 2 из certificate_ca.crt на 19 строчек
-----BEGIN CERTIFICATE-----
MIIDXzCCAkegAw...
WD9f
-----END CERTIFICATE-----
```

### нужно скопировать 2 файла сертификатов в папку с сертификатами и перезагрузить nginx
```bash
scp ./certificate.pem root@79.174.94.70:/etc/nginx/certs
# пароль от сервера
scp ./certificate.key root@79.174.94.70:/etc/nginx/certs
# пароль от сервера
```

### заходим на стенд
```bash
ssh root@79.174.94.70
пароль от сервера
```

### перезагружаем nginx
```bash
sudo systemctl restart nginx
```

## сделать github ci для бека и фронта
### для бека сделал
### для фронта надо добавить параметры сервера в креды github actions (устал Кирилла об этом просить) или дать мне привилегии на это
### как будто это нафиг не нужно, тк никто не обновляет ничего...
### разработка пойдет - быстро сделаем...
```yaml
host: ${{ secrets.SERVER_HOST }}
username: ${{ secrets.SERVER_USER }}
key: ${{ secrets.SERVER_SSH_KEY }}
```
## коллекцию постмана

### как потребуется, так скину

## передать сгенеренные qr в телегу

### вроде передавал, но по базе их можно перегенерить при необходимости

