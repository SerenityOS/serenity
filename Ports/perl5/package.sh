#!/usr/bin/env -S bash ../.port_include.sh
port='perl5'
version='5.40.1'
cross_version='1.6.1'
useconfigure='true'
files=(
    "https://www.cpan.org/src/5.0/perl-${version}.tar.xz#dfa20c2eef2b4af133525610bbb65dd13777ecf998c9c5b1ccf0d308e732ee3f"
    "https://github.com/arsv/perl-cross/releases/download/${cross_version}/perl-cross-${cross_version}.tar.gz#b5f4b4457bbd7be37adac8ee423beedbcdba8963a85f79770f5e701dabc5550f"
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
