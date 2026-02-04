#!/bin/sh


keytool -genkey -v -keystore key.jks -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 -alias key
keytool -exportcert -alias key -keystore "key.jks" | openssl sha1 -binary | openssl base64
