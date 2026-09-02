#!/usr/bin/env -S bash ../.port_include.sh
port='npth'
version='1.8'
useconfigure='true'
files=(
    "https://gnupg.org/ftp/gcrypt/npth/npth-${version}.tar.bz2#8bd24b4f23a3065d6e5b26e98aba9ce783ea4fd781069c1b35d149694e90ca3e"
)

configure() {
    run ./configure --host="${SERENITY_ARCH}-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
