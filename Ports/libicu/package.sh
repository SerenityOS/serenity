#!/usr/bin/env -S bash ../.port_include.sh
port='libicu'
version='77.1'
useconfigure='true'
use_fresh_config_sub='true'
workdir='icu/source'
files=(
    "https://github.com/unicode-org/icu/releases/download/release-${version//./-}/icu4c-${version//./_}-src.tgz#588e431f77327c39031ffbb8843c0e3bc122c211374485fa87dc5f3faff24061"
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

export CFLAGS='-DU_HAVE_NL_LANGINFO_CODESET=0 -DU_HAVE_CHAR16_T=0'
export CXXFLAGS='-DU_HAVE_NL_LANGINFO_CODESET=0 -DU_HAVE_CHAR16_T=0'
