#!/usr/bin/env -S bash ../.port_include.sh
port='perl5'
version='5.40.0'
cross_version='1.5.3'
useconfigure='true'
files=(
    "https://www.cpan.org/src/5.0/perl-${version}.tar.xz#d5325300ad267624cb0b7d512cfdfcd74fa7fe00c455c5b51a6bd53e5e199ef9"
    "https://github.com/arsv/perl-cross/releases/download/${cross_version}/perl-cross-${cross_version}.tar.gz#ecc37b41a60cc3c030413a960cc386455f70c43781c6333d1fcaad02ece32ea8"
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
