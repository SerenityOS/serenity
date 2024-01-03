#!/usr/bin/env -S bash ../.port_include.sh
port='perl5'
version='5.38.2'
cross_version='1.5.2'
useconfigure='true'
files=(
    "https://www.cpan.org/src/5.0/perl-${version}.tar.xz#d91115e90b896520e83d4de6b52f8254ef2b70a8d545ffab33200ea9f1cf29e8"
    "https://github.com/arsv/perl-cross/releases/download/${cross_version}/perl-cross-${cross_version}.tar.gz#584dc54c48dca25e032b676a15bef377c1fed9de318b4fc140292a5dbf326e90"
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
    run cp -r "${PWD}/perl-cross-${cross_version}/"* "${PWD}/perl-${version}/"

    if [ "$(uname -s)" = 'Darwin' ]; then
        cat <<- 'EOH' > "${PWD}/perl-${version}/readelf"
#!/bin/bash
exec gobjdump "$@"
EOH
        chmod +x "${PWD}/perl-${version}/readelf"
    fi
}
