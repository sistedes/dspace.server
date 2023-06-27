# Fix solr permissions
chown -R solr:solr /var/solr/data/configsets

# Fix server permissions
# Tomcat must be configured first using `systemctl edit tomcat9.service` as:
#
# [Service]
# ReadWritePaths=/opt/dspace-server
# Environment='UMASK=002'
server=/opt/dspace.server

chmod g+x -R $server/bin
find $server -type d -exec chmod g+s {} \;
find $server -type f -exec chmod g-s {} \;
chmod ug+w -R $server
chown dspace:tomcat -R $server
