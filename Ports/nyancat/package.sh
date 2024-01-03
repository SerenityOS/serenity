#!/usr/bin/env -S bash ../.port_include.sh
port='nyancat'
version='git'
commit_hash='5ffb6c5c03d0e9156db8f360599d4f0449bb16b9'
workdir="nyancat-${commit_hash}"
files=(
    "https://github.com/klange/nyancat/archive/${commit_hash}.tar.gz#d9c3ea82ce59f0d7db86db9e8a626f8f8fa2fbd9544104557e4c59a31893ca31"
)
launcher_name='Nyancat'
launcher_category='&Games'
launcher_command='/usr/local/bin/nyancat'
launcher_run_in_terminal='true'
