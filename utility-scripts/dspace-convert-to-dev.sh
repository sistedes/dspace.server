#! /bin/bash

sed -i 's/biblioteca.sistedes.es/bdsistedes.dsic.upv.es/g' /opt/dspace.ui/config/config.prod.yml
sed -i 's/biblioteca.sistedes.es/bdsistedes.dsic.upv.es/g' /opt/dspace.server/config/local.cfg

systemctl restart tomcat9.service
pm2 restart all
