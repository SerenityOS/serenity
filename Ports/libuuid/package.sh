#!/usr/bin/env -S bash ../.port_include.sh

port=libuuid
version=2.38
workdir="util-linux-${version}"
useconfigure=true
configopts=("--prefix=/usr/local" "--disable-all-programs" "--enable-libuuid")
files="https://mirrors.edge.kernel.org/pub/linux/utils/util-linux/v${version}/util-linux-${version}.tar.gz util-linux-${version}.tar.gz c31d4e54f30b56b0f7ec8b342658c07de81378f2c067941c2b886da356f8ad42
https://git.savannah.gnu.org/cgit/config.git/plain/config.sub?id=c179db1b6f2ae484bfca1e9f8bae273e3319fa7d config.sub deb02c26f43b2ea64276c9ede77ec0f53d08e6256710f3c0a12275712085c348"
auth_type=sha256

pre_configure() {
    # The generated config.sub is a *symlink* to the host's.
    # The one from GNU config should be similar enough, and recognizes serenity.
    run rm -f config/config.sub
    run cp ../config.sub config
}
