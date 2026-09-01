#!/usr/bin/env -S bash ../.port_include.sh
port='libicu'
version='78.3'
useconfigure='true'
use_fresh_config_sub='true'
workdir='icu/source'
files=(
    "https://github.com/unicode-org/icu/releases/download/release-${version}/icu4c-${version}-sources.tgz#3a2e7a47604ba702f345878308e6fefeca612ee895cf4a5f222e7955fabfe0c0"
)

configure() {
    host_env
    run mkdir -p ../host-build
    run sh -c "cd ../host-build && ../source/configure && make ${makeopts[*]}"
    target_env
    run ./configure \
        --host="${SERENITY_ARCH}-serenity" \
        --with-cross-build="$(pwd)/${workdir}/../host-build"
}

export CFLAGS='-DU_HAVE_NL_LANGINFO_CODESET=0 -DU_HAVE_CHAR16_T=0'
export CXXFLAGS='-DU_HAVE_NL_LANGINFO_CODESET=0 -DU_HAVE_CHAR16_T=0'
