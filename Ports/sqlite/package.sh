#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3.54.0'
# Download URL uses a different version format
_version='3540000'
files=(
    "https://www.sqlite.org/2026/sqlite-autoconf-${_version}.tar.gz#134ec0802dda5795816e25d25872d20b312cb3973438c49b30bc40b7705ea9ed"
)
workdir="sqlite-autoconf-${_version}"
useconfigure='true'
launcher_name='SQLite'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/sqlite3 -interactive'
launcher_run_in_terminal='true'
#icon_file=FIXME
