#! /bin/bash

sourceui=/root/git/dspace.ui
ui=/opt/dspace.ui

pushd $sourceui
yarn merge-i18n -s src/themes/sistedes/assets/i18n
yarn build:prod
popd

mkdir -p $ui/dist
rm -rf $ui/dist/*
cp -r $sourceui/dist $ui

pushd $ui
pm2 stop dspace.ui
pm2 start dspace.ui.json
pm2 save
popd
