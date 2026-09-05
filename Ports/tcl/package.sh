#!/usr/bin/env -S bash ../.port_include.sh
port='tcl'
version='9.0.4'
workdir="tcl${version}/unix"
useconfigure='true'
launcher_name='Tcl'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/tclsh9.0'
launcher_run_in_terminal='true'
#icon_file=FIXME
files=(
    "https://prdownloads.sourceforge.net/tcl/tcl${version}-src.tar.gz#d0aed49230bc02a65c1e0229e65f34590a4b037ec40d546f32573b467f7551ea"
)

post_configure() {
    # Vendored sqlite3 was pre-configured on someone else's machine...?
    # PKG_CFLAGS="$PKG_CFLAGS -DSQLITE_USE_ALLOCA=1"
    run_replace_in_file 's/-DSQLITE_USE_ALLOCA=1//' ../pkgs/sqlite3.53.0/configure
}
