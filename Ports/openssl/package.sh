#!/usr/bin/env -S bash ../.port_include.sh
port='openssl'
branch='1.1.1'
version="${branch}s"
useconfigure='true'
configscript='Configure'
files="https://www.openssl.org/source/openssl-${version}.tar.gz openssl-${version}.tar.gz c5ac01e760ee6ff0dab61d6b2bbd30146724d063eb322180c6f18a6f74e4b6aa"
auth_type='sha256'
depends=(
    'zlib'
)

COMPILE_TESTS='true'

configopts=(
    "--prefix=/usr/local"
    "--openssldir=/usr/local/ssl"
    "-DOPENSSL_SYS_SERENITY=1"
    "-DOPENSSL_USE_IPV6=0"
    "zlib"
    "threads"
    "no-asm"
    "serenity-generic"
)
if [ "${COMPILE_TESTS:-false}" = "true" ]; then
    makeopts=("HOST_CC=${HOST_CC} ${makeopts} -fprofile-arcs -ftest-coverage")
    configopts+=(
        'enable-unit-test'
    )
    # depends+=(
    # 'perl'
    # )
else
    configopts+=(
        'no-tests'
    )
fi

configure() {
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
    run ./"$configscript" "${configopts[@]}"
}

install() {
    # The default "install" also installs docs, which we don't want.
    run make DESTDIR=$DESTDIR install_sw "${installopts[@]}"
    run make DESTDIR=$DESTDIR install_ssldirs "${installopts[@]}"
    if [ "${COMPILE_TESTS:-false}" = "true" ]; then
        # run make DESTDIR="${DESTDIR}" tests "${installopts[@]}" || true
        run mkdir -p ${SERENITY_INSTALL_ROOT}/home/anon/port_tests/openssl
        # run cp -r test ${SERENITY_INSTALL_ROOT}/home/anon/port_tests/openssl
        run cp -r . ${SERENITY_INSTALL_ROOT}/home/anon/port_tests/openssl
        # run make DESTDIR=$DESTDIR test HARNESS_JOBS=${HARNESS_JOBS:-4}
    fi
}
