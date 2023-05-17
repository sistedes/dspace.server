#! /bin/bash

server="/opt/dspace.server"

systemctl stop tomcat9.service
sudo -u postgres psql --username=postgres dspace -c "DROP EXTENSION pgcrypto CASCADE;"
yes | $server/bin/dspace database clean
sudo -u postgres psql --username=postgres dspace -c "CREATE EXTENSION pgcrypto;"
rm -rf $server/assetstore/*
/opt/dspace-fix-permissions.sh
/opt/dspace-create-admin.sh
systemctl start tomcat9.service
"$server/bin/dspace" initialize-entities -f "$server/config/entities/sistedes-relationship.xml"

