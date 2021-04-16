#!/usr/bin/env -S bash ../.port_include.sh
port=libicu
version=69_1
useconfigure=true
workdir=icu/source
configopts=--with-cross-build=$(pwd)/${workdir}/../host-build
files="https://github.com/unicode-org/icu/releases/download/release-${version//_/-}/icu4c-${version}-src.tgz icu4c-${version}-src.tgz 9403db682507369d0f60a25ea67014c4"
auth_type=md5

configure() {
    host_env
    run mkdir -p ../host-build
    run sh -c "cd ../host-build && ../source/configure && make $makeopts"
    target_env
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" $configopts
}

export CFLAGS="-DU_HAVE_NL_LANGINFO_CODESET=0 -DLC_MESSAGES=1"
export CXXFLAGS="-DU_HAVE_NL_LANGINFO_CODESET=0 -DLC_MESSAGES=1"
