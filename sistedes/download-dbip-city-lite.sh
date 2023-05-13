#! /bin/bash

year=`date +'%Y'`
month=`date +'%m'`
target="/opt/dbip-city-lite"
outgz="$target/dbip-city-lite.mmdb.gz"

mkdir -p $target
wget -O $outgz "https://download.db-ip.com/free/dbip-city-lite-$year-$month.mmdb.gz"
gunzip $outgz