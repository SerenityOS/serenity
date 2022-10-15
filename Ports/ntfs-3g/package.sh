#!/usr/bin/env -S bash ../.port_include.sh
port='ntfs-3g'
version='2022.5.17'
useconfigure='true'
files="https://github.com/tuxera/ntfs-3g/archive/refs/tags/${version}.zip ntfs-3g-${version}.zip d953594f4d947ebe0ddf34740926e5508f440b868b5381fa6e51e491824fb5ba"
auth_type='sha256'
configopts=("--enable-shared" "--prefix=${DESTDIR}/usr/local" "--oldincludedir=${DESTDIR}/usr/include/" "--with-fuse=external")

pre_configure() {
    run ./autogen.sh
}

build() {
    run make ntfsprogs
}

install() {
    run make -C ntfsprogs install
}

post_install() {
    ln -sf /usr/local/sbin/mkntfs "${DESTDIR}/usr/local/sbin/mkfs.ntfs"
}
