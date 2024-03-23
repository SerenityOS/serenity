#!/usr/bin/env -S bash ../.port_include.sh

port='rvvm'
version='0.6'
archive_hash='97e98c95d8785438758b81fb5c695b8eafb564502c6af7f52555b056e3bb7d7a'
files=(
    "https://github.com/LekKit/RVVM/archive/v${version}.tar.gz#${archive_hash}"
)
workdir="RVVM-${version}"
depends=('SDL2')

build_opts=(
    'GIT_COMMIT=f937fd8'
    'OS=SerenityOS'
    'USE_SDL=2'
)
makeopts+=("${build_opts[@]}" 'all' 'lib')
installopts+=("${build_opts[@]}")
