#!/usr/bin/env -S bash ../.port_include.sh
port='qemu'
version='7.2.0'
useconfigure='true'
configopts=(
    '--target-list=aarch64-softmmu,i386-softmmu,x86_64-softmmu'
    "--cross-prefix=${SERENITY_ARCH}-pc-serenity-"
    '--extra-ldflags=-lm'
    '--without-default-features'
    '--disable-strip'
    '--enable-pie'
    '--enable-sdl'
    '--enable-slirp'
    '--enable-tcg'
    '--enable-tools'
)
depends=(
  'glib'
  'libslirp'
  'pixman'
  'SDL2'
)
files="https://download.qemu.org/qemu-${version}.tar.xz qemu-${version}.tar.xz 5b49ce2687744dad494ae90a898c52204a3406e84d072482a1e1be854eeb2157"
auth_type='sha256'

pre_patch() {
    # Disable tests (those need way more stuff than QEMU itself) by clearing the respective meson file.
    echo '' > "${workdir}/tests/meson.build"
}

post_install() {
    # Add a drop-in fstab entry to make sure that we can use anonymous executable memory and bypass W^X
    mkdir -p "${SERENITY_INSTALL_ROOT}/etc/fstab.d"
    rm -rf "${SERENITY_INSTALL_ROOT}/etc/fstab.d/qemu"
    for i in /usr/local/bin/qemu-system-{aarch64,i386,x86_64}; do
        echo "${i}	${i}	bind	bind,wxallowed,axallowed" >> "${SERENITY_INSTALL_ROOT}/etc/fstab.d/qemu"
    done
}

# We don't have '<arch>-pc-serenity-pkg-config', so just use the "normal" one.
export PKG_CONFIG="pkg-config"
