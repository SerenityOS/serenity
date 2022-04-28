#!/usr/bin/env -S bash ../.port_include.sh

port=libuuid
version=2.38
workdir="util-linux-${version}"
useconfigure=true
configopts=("--prefix=/usr/local" "--disable-all-programs" "--enable-libuuid")
files="https://github.com/karelzak/util-linux/archive/refs/tags/v${version}.tar.gz util-linux-${version}.tar.gz f3cf9d165f50f46e5c0a1076d178a75a5ae30463345e9c19335552b249fe0e67
https://git.savannah.gnu.org/cgit/config.git/plain/config.sub?id=c179db1b6f2ae484bfca1e9f8bae273e3319fa7d config.sub deb02c26f43b2ea64276c9ede77ec0f53d08e6256710f3c0a12275712085c348"
auth_type=sha256

pre_configure() {
    # NOTE: This requires various tools to exist on the host machine (it'll tell you).
    run ./autogen.sh

    # The generated config.sub is a *symlink* to the host's.
    # The one from GNU config should be similar enough, and recognizes serenity.
    run rm -f config/config.sub
    run cp ../config.sub config
}
