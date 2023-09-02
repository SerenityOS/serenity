#!/usr/bin/env -S bash ../.port_include.sh
port='perl5'
version='5.38.0'
useconfigure='true'
files=(
    "https://www.cpan.org/src/5.0/perl-${version}.tar.xz#eca551caec3bc549a4e590c0015003790bdd1a604ffe19cc78ee631d51f7072e"
    "https://github.com/arsv/perl-cross/releases/download/1.5/perl-cross-1.5.tar.gz#d744a390939e2ebb9a12f6725b4d9c19255a141d90031eff90ea183fdfcbf211"
)
configopts=(
    '-Dosname=serenity'
    "--target=${SERENITY_ARCH}-pc-serenity"
    "--targetarch=${SERENITY_ARCH}"
    "--build=$(cc -dumpmachine)"
    "--buildarch=$(uname -m)"
    '--prefix=/usr/local'
    "--sysroot="${SERENITY_INSTALL_ROOT}""
)
workdir="perl-${version}"

if [ "$(uname -s)" = 'Darwin' ]; then
    makeopts+=('--ignore-errors')
    # Make sure you have binutils and gnu-sed installed via homebrew
    PATH="$(brew --prefix binutils)/bin:${PATH}"
    PATH="$(brew --prefix gnu-sed)/libexec/gnubin:${PATH}"
    export PATH="${SERENITY_BUILD_DIR}/Ports/${port}/${workdir}:${PATH}"
fi

post_fetch() {
    run chmod -R +rw "${PWD}/perl-${version}"*
    run cp -r "${PWD}/perl-cross-1.5/"* "${PWD}/perl-${version}/"

    if [ "$(uname -s)" = 'Darwin' ]; then
        cat <<- 'EOH' > "${PWD}/perl-${version}/readelf"
#!/bin/bash
exec gobjdump "$@"
EOH
        chmod +x "${PWD}/perl-${version}/readelf"
    fi
}
