#!/bin/sh

sudo apt update
sudo apt install nginx
systemctl status nginx

sudo apt install nano
sudo apt install git

sudo mkdir -p /var/www/car_id
sudo mkdir -p /var/www/car_id/jar
sudo chmod 777 /var/www/car_id/jar


#____________nginx________________

sudo tee /etc/nginx/sites-available/car_id <<EOF
server {
    listen 80;
    listen [::]:80;

    server_name gachi-huyachi.fun www.gachi-huyachi.fun;
    root /var/www/car_id/front;
    index index.html;

    location ~ "^/\$" {
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        if ( \$request_method = POST ) {
            proxy_pass http://127.0.0.1:8081;
        }
    }

    location / {
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location ~ "^/qr/.*\$" {
       try_files \$uri /qr.html;
    }

    location ~ "^/notification/.*\$" {
       try_files \$uri /notification.html;
    }

    location /api {
        add_header 'Access-Control-Allow-Origin' '*' always;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto http;
        proxy_pass http://127.0.0.1:8081;
    }

    location /swagger-ui {
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto http;
        proxy_pass http://127.0.0.1:8081;
    }

    location /v3 {
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto http;
        proxy_pass http://127.0.0.1:8081;
    }

    location /privacy-policy {
       try_files \$uri /privacy-policy.html;
    }

    location /offer {
       try_files \$uri /offer.html;
    }

    location /config {
      try_files \$uri \$uri/ =404;
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/car_id /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx


#________________JDK___18______________________

sudo apt install -y curl wget
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz
tar -xvf jdk-21_linux-x64_bin.tar.gz
sudo mkdir -p /usr/lib/jvm
sudo mv jdk-21.0.3 /usr/lib/jvm
rm -rf jdk-21_linux-x64_bin.tar.gz

sudo tee /etc/profile.d/jdk21.sh <<EOF
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.3
export PATH=\$PATH:\$JAVA_HOME/bin
EOF

source /etc/profile.d/jdk21.sh
echo $JAVA_HOME
java -version


#_______________JAVA_BACK_SERVICE__________________________

sudo tee /var/www/car_id/run.sh <<EOF
#!/bin/bash
/usr/lib/jvm/jdk-21.0.3/bin/java \\
  -Dspring.profiles.active=dev \\
  -jar /var/www/car_id/jar/car_id.jar
EOF


# сервис для бека, который раннер будет перезапускать
sudo tee /etc/systemd/system/car_id.service <<EOF
[Unit]
Description=car_id_backend
After=syslog.target network.target
[Service]
ExecStart=/bin/bash /var/www/car_id/run.sh
ExecStop=/bin/kill -HUP \$MAINPID
Restart=always
KillMode=process
KillSignal=SIGTERM
# Набор переменных окружений
# Environment=TELEGRAM_TOKEN=Foo
WorkingDirectory=/var/www/car_id
Environment=LOG_PATH=/var/www/car_id
PIDFile=/var/www/car_id/car_id.pid
[Install]
WantedBy=multi-user.target
EOF

chmod 774 /var/www/car_id/run.sh

systemctl enable car_id.service
systemctl start car_id.service
systemctl status car_id.service

journalctl -u car_id.service


sudo tee /etc/systemd/system/deploy.service <<EOF
[Unit]
Description=Deploy
After=syslog.target network.target
[Service]
User=root
WorkingDirectory=/var/www/car_id/jar
ExecStart=/bin/bash -c 'if [ -f carId.jar ]; then /usr/bin/mv carId.jar car_id.jar; /usr/bin/systemctl kill car_id.service; fi'
KillMode=process
KillSignal=SIGTERM
Environment=JAVA_HOME=/usr/lib/jvm/jdk-21.0.3
[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload

systemctl enable deploy.service
systemctl start deploy.service
systemctl status deploy.service


#_______________fail2ban__________________________
sudo apt install fail2ban
sudo systemctl enable fail2ban

sudo tee /etc/fail2ban/jail.local <<EOF
[ssh]
enabled   = true
port     = ssh
filter   = sshd
maxretry  = 6
findtime  = 1h
bantime   = 1d
ignoreip  = 127.0.0.1/8
logpath   = /var/log/auth.log
EOF

touch /var/log/auth.log
chmod 777 /var/log/auth.log


service fail2ban restart
systemctl status fail2ban
sudo fail2ban-client status sshd

#_______________postgres dump__________________________

sudo mkdir -p /var/www/car_id/backups

sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt update
sudo apt install postgresql-client-15

sudo tee /root/.pgpass <<EOF
host:port:dbname:user:password
EOF
sudo chmod 600 /root/.pgpass

#вставляем скрипт ...
nano /var/www/car_id/backup.sh

sudo chmod +x /var/www/car_id/backup.sh
# запускаем и проверяем
sudo /var/www/car_id/backup.sh

#_______________postgres dump service__________________________

sudo tee /etc/systemd/system/backup.service <<EOF
[Unit]
Description=Backup
After=syslog.target network.target
[Service]
User=root
WorkingDirectory=/var/www/car_id
ExecStart=/bin/bash /var/www/car_id/backup.sh
KillMode=process
KillSignal=SIGTERM
[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload

systemctl enable backup.service
systemctl start backup.service
systemctl status backup.service

sudo tee /usr/lib/systemd/system/backup.timer <<EOF
[Unit]
Description=Run my task daily at 3 AM

[Timer]
OnCalendar=03:00

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable backup.timer
systemctl start backup.timer
systemctl list-timers

#_______________health check service__________________________

sudo tee /etc/systemd/system/health.service <<EOF
[Unit]
Description=Backup
After=syslog.target network.target
[Service]
User=root
WorkingDirectory=/var/www/car_id
ExecStart=/bin/bash /var/www/car_id/health.sh
KillMode=process
KillSignal=SIGTERM
[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload

systemctl enable health.service
systemctl start health.service
systemctl status health.service

sudo tee /usr/lib/systemd/system/health.timer <<EOF
[Unit]
Description=Run my task each 5 second

[Timer]
OnBootSec=5
OnUnitActiveSec=5
AccuracySec=1ms
Unit=health.service

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable health.timer
systemctl start health.timer
systemctl list-timers