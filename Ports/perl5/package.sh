#!/usr/bin/env -S bash ../.port_include.sh
port='perl5'
version='5.44.0'
cross_version='1.6.5'
useconfigure='true'
files=(
    "https://www.cpan.org/src/5.0/perl-${version}.tar.xz#505cf43912e9480495c344c70260452e32aa2a73c546a026b3f100053b23ce91"
    "https://github.com/arsv/perl-cross/releases/download/${cross_version}/perl-cross-${cross_version}.tar.gz#81130cd4b8c6d9eb2a1959f37d44391a46ad6a6794fc41fed5441f74e85e3dd0"
)
configopts=(
    '-Dosname=serenity'
    "--target=${SERENITY_ARCH}-serenity"
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
