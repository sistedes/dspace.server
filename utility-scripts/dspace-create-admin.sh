#!/usr/bin/expect -f

set server /opt/dspace.server

set timeout -1
spawn $server/bin/dspace create-administrator
match_max 100000
expect "E-mail address: "
send -- "$env(DSPACE_USER)\r"
expect "First name: "
send -- "Biblioteca Digital\r"
expect "Last name: "
send -- "Sistedes\r"
expect "Is the above data correct? (y or n): "
send -- "y\r"
expect "Password: "
send -- "$env(DSPACE_PASS)\r"
expect "Again to confirm: "
send -- "$env(DSPACE_PASS)\r"
expect eof
