#!/usr/bin/env -S bash ../.port_include.sh
port=openttd-opensfx
version=1.0.1
workdir=.
files="https://cdn.openttd.org/opensfx-releases/${version}/opensfx-${version}-all.zip opensfx-${version}-all.zip 37b825426f1d690960313414423342733520d08916f512f30f7aaf30910a36c5"
auth_type=sha256

build() {
    # The ZIP file we downloaded contains a tarball.
    run_nocd tar xf opensfx-${version}.tar
}

install() {
    run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
    run_nocd cp -a opensfx-${version}/* ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
}
