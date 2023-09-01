#!/usr/bin/env -S bash ../.port_include.sh
port='libicu'
version='73.2'
useconfigure='true'
use_fresh_config_sub='true'
workdir='icu/source'
files=(
    "https://github.com/unicode-org/icu/releases/download/release-${version//./-}/icu4c-${version//./_}-src.tgz#818a80712ed3caacd9b652305e01afc7fa167e6f2e94996da44b90c2ab604ce1"
)

configure() {
    host_env
    run mkdir -p ../host-build
    run sh -c "cd ../host-build && ../source/configure && make ${makeopts[*]}"
    target_env
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-cross-build="$(pwd)/${workdir}/../host-build"
}

export CFLAGS='-DU_HAVE_NL_LANGINFO_CODESET=0'
export CXXFLAGS='-DU_HAVE_NL_LANGINFO_CODESET=0'
