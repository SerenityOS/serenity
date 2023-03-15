#!/usr/bin/env -S bash ../.port_include.sh
port='lxt'
version='1.3b'
files="http://stahlke.org/dan/lxt/lxt-${version}.tar.gz lxt-${version}.tar.gz 7a3ab299a6d61a71b271fd13b847b5a1c22a5f95df78561a325c78d50b6a6bc7"
useconfigure='true'
auth_type='sha256'
depends=('ncurses'
	'bash')

pre_configure() {
    # Rebuild after patching configure.ac to support serenity host.
    # `automake` may exit with a warning about how there is a mismatch
    # between the versions of autoconf and automake that were previously
    # used to generate aclocal and specifed in configure.ac.
    # We just need `automake` to generate `./compile` (so that we can run
    # autoreconf to regenerate everything).
    run aclocal
    run automake --add-missing
    run autoreconf
}
