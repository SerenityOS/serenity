#!/usr/bin/env -S bash ../.port_include.sh
port='jdupes'
version='1.26.1'
files=(
    "https://github.com/jbruchon/jdupes/archive/refs/tags/v${version}.tar.gz#09153824320c65ad529f8f97cd3b7e792c50e9f9018192ea1a76f2e33a196225"
)
auth_type='sha256'
workdir="jdupes-${version}"
depends=('libjodycode')
makeopts+=("UNAME_S=serenity")
installopts+=('DISABLE_DEDUPE=1')
export LDFLAGS="-z noexecstack"
