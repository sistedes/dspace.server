#! /bin/bash

prodsite=biblioteca.sistedes.es
devsite=bdsistedes.dsic.upv.es
cookies=/tmp/bdcookies.tmp
headers=/tmp/bdheaders.tmp
user=USER_EMAIL
password=USER_PASSWORD

sed -i "s/$prodsite/$devsite/g" /opt/dspace.ui/config/config.prod.yml
sed -i "s/$prodsite/$devsite/g" /opt/dspace.server/config/local.cfg

function get_header() {
  cat /tmp/bdheaders.tmp | grep $1 | cut -d ':' -f2 | xargs
}

curl --silent \
     --output /dev/null \
     --cookie-jar $cookies \
     --dump-header $headers \
     'http://localhost:8080/dspace.server/api/'

token=$(get_header DSPACE-XSRF-TOKEN)

curl --silent \
     --output /dev/null \
     --cookie $cookies \
     --cookie-jar $cookies \
     --dump-header $headers \
     'http://localhost:8080/dspace.server/api/authn/login' \
     -H "X-XSRF-TOKEN: $token" \
     --data-urlencode "user=$user" \
     --data-urlencode "password=$password"

token=$(get_header DSPACE-XSRF-TOKEN)
auth=$(get_header Authorization)

curl --silent \
     --output /dev/null \
     --cookie $cookies \
     --cookie-jar $cookies \
     --dump-header $headers \
     -X PUT \
     'http://localhost:8080/dspace.server/api/system/systemwidealerts/1' \
     -H "Content-Type: application/json" \
     -H "X-XSRF-TOKEN: $token" \
     -H "Authorization: $auth" \
     --data '{ "message": "<b>Este es un sitio de desarrollo y pruebas</b>", "countdownTo": null, "allowSessions": "all", "active": true }'

rm -rf $cookies $headers

systemctl restart tomcat9.service
pm2 restart all
