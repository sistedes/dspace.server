#!/usr/bin/expect -f

set server /opt/dspace.server
set email HERE_THE_EMAIL
set password HERE_THE_PASSWORD

set timeout -1
spawn $server/bin/dspace create-administrator
match_max 100000
expect "E-mail address: "
send -- "$email\r"
expect "First name: "
send -- "Biblioteca Digital\r"
expect "Last name: "
send -- "Sistedes\r"
expect "Is the above data correct? (y or n): "
send -- "y\r"
expect "Password: "
send -- "$password\r"
expect "Again to confirm: "
send -- "$password\r"
expect eof
