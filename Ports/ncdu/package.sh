#!/usr/bin/env -S bash ../.port_include.sh
port='ncdu'
version='1.17'
files="https://dev.yorhel.nl/download/ncdu-${version}.tar.gz ncdu-${version}.tar.gz 810745a8ed1ab3788c87d3aea4cc1a14edf6ee226f764bcc383e024ba56adbf1"
auth_type='sha256'
useconfigure='true'
depends=("ncurses")
