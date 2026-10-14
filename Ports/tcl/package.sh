#!/usr/bin/env -S bash ../.port_include.sh
port='tcl'
version='9.1.0'
workdir="tcl${version}/unix"
useconfigure='true'
launcher_name='Tcl'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/tclsh9.1'
launcher_run_in_terminal='true'
#icon_file=FIXME
files=(
    "https://downloads.sourceforge.net/project/tcl/Tcl/${version}/tcl${version}-src.tar.gz#536c45543f64d6eb11832d97ba3494aacff046fbc5040273bd55258d0e448ff1"
)

post_configure() {
    # Vendored sqlite3 was pre-configured on someone else's machine...?
    # PKG_CFLAGS="$PKG_CFLAGS -DSQLITE_USE_ALLOCA=1"
    run_replace_in_file 's/-DSQLITE_USE_ALLOCA=1//' ../pkgs/sqlite3.53.4/configure
}
