#!/usr/bin/env -S bash ../.port_include.sh
port=openttd-opengfx
version=0.6.1
workdir=.
files="https://cdn.openttd.org/opengfx-releases/${version}/opengfx-${version}-all.zip opengfx-${version}-all.zip c694a112cd508d9c8fdad1b92bde05e7c48b14d66bad0c3999e443367437e37e"
auth_type=sha256

build() {
    # The ZIP file we downloaded contains a tarball.
    run_nocd tar xf opengfx-${version}.tar
}

install() {
    run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
    run_nocd cp -a opengfx-${version}/* ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
}
