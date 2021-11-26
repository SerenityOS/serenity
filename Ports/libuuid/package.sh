#!/usr/bin/env -S bash ../.port_include.sh

port=libuuid
version=2.37.2
workdir="util-linux-${version}"
useconfigure=true
configopts=("--prefix=/usr/local" "--disable-all-programs" "--enable-libuuid")
files="https://github.com/karelzak/util-linux/archive/refs/tags/v${version}.tar.gz util-linux-${version}.tar.gz 74e725802a6355bba7288caeca171e0e25d9da2aa570162efbc1397ed924dfa2
https://git.savannah.gnu.org/cgit/config.git/plain/config.sub?id=2707e389a5c8ec14db468cdc4979864cd57b53f5 config.sub 698198944a59f5915b3f68dc9d642f573aeb8960307493a5693e6b148d5bb4c6"
auth_type=sha256

pre_configure() {
    # NOTE: This requires various tools to exist on the host machine (it'll tell you).
    run ./autogen.sh

    # The generated config.sub is a *symlink* to the host's.
    # The one from GNU config should be similar enough, and recognizes serenity.
    run rm -f config/config.sub
    run cp ../config.sub config
}
