#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3470000'
files=(
    "https://www.sqlite.org/2024/sqlite-autoconf-${version}.tar.gz#83eb21a6f6a649f506df8bd3aab85a08f7556ceed5dbd8dea743ea003fc3a957"
)
useconfigure='true'
launcher_name='SQLite'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/sqlite3 -interactive'
launcher_run_in_terminal='true'
icon_file='https://static-00.iconduck.com/assets.00/file-type-sqlite-icon-1831x2048-xsspcqq6.png'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
