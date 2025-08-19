#! /bin/bash

dir=`dirname "$(realpath $0)"`
source "$dir/environment.sh"

server="/opt/dspace.server"

systemctl stop tomcat10.service
yes | $server/bin/dspace database clean
rm -rf $server/assetstore/*
/opt/dspace-fix-permissions.sh
"$server/bin/dspace" initialize-entities -f "$server/config/entities/sistedes-relationship.xml"
/opt/dspace-create-admin.sh
systemctl start tomcat10.service

