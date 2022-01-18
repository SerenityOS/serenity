#!/usr/bin/env -S bash ../.port_include.sh
port=tig
version=2.5.5
useconfigure="true"
files="https://github.com/jonas/tig/releases/download/tig-${version}/tig-${version}.tar.gz tig-${version}.tar.gz 24ba2c8beae889e6002ea7ced0e29851dee57c27fde8480fb9c64d5eb8765313"
auth_type=sha256
depends=("ncurses" "pcre" "readline")
