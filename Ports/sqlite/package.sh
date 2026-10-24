#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3.54.1'
# Download URL uses a different version format
_version='3540100'
files=(
    "https://www.sqlite.org/2026/sqlite-autoconf-${_version}.tar.gz#926ef8d3eab93fbf6e06011901273eec076b25594a4e1cdadd50c987f4f6d64e"
)
workdir="sqlite-autoconf-${_version}"
useconfigure='true'
launcher_name='SQLite'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/sqlite3 -interactive'
launcher_run_in_terminal='true'
#icon_file=FIXME
