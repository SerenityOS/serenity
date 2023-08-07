#!/usr/bin/env -S bash ../.port_include.sh
port='rvvm'
version='0.5'
archive_hash='3a1dbb91ad04f068078bc6c6c27cc5792eebc111907cb5a14bde158fe6e757c9'
files=(
    "https://github.com/LekKit/RVVM/archive/v${version}.tar.gz v${version}.tar.gz ${archive_hash}"
)
workdir="RVVM-${version}"
depends=('sdl12-compat')

build_opts=(
    'GIT_COMMIT=76796ba'
    'OS=SerenityOS'
    'USE_NET=1'
    'USE_SDL=1'
)
makeopts+=("${build_opts[@]}" 'all' 'lib')
installopts+=("${build_opts[@]}")
