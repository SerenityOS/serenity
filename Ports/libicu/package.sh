#!/usr/bin/env -S bash ../.port_include.sh
port='libicu'
version='79.1'
useconfigure='true'
use_fresh_config_sub='true'
workdir='icu/source'
files=(
    "https://github.com/unicode-org/icu/releases/download/release-${version}/icu4c-${version}-sources.tgz#06fa63ecb521048f8082f12ad988f6b678d90cbd88aafce24cbc94902b191d99"
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
