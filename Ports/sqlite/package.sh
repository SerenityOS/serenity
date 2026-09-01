#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3.53.4'
# Download URL uses a different version format
_version='3530400'
files=(
    "https://www.sqlite.org/2026/sqlite-autoconf-${_version}.tar.gz#0e9483900e92cd5de8fd48d16bf9200145a61f7fd5be542a5ac81d8a9516eb9c"
)
workdir="sqlite-autoconf-${_version}"
useconfigure='true'
launcher_name='SQLite'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/sqlite3 -interactive'
launcher_run_in_terminal='true'
#icon_file=FIXME
