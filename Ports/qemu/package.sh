#!/usr/bin/env -S bash ../.port_include.sh
port='qemu'
version='7.0.0'
useconfigure='true'
configopts=(
    "--target-list=aarch64-softmmu,i386-softmmu,x86_64-softmmu"
    "--cross-prefix=${SERENITY_ARCH}-pc-serenity-"
    "--extra-ldflags=-lm"
    "--without-default-features"
    "--disable-strip"
    "--enable-pie"
    "--enable-sdl"
    "--enable-tcg"
    "--enable-tools"
)
depends=("glib" "pixman" "SDL2")
files="https://download.qemu.org/qemu-${version}.tar.xz qemu-${version}.tar.xz f6b375c7951f728402798b0baabb2d86478ca53d44cedbefabbe1c46bf46f839"
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
