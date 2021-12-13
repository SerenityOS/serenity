#!/usr/bin/env -S bash ../.port_include.sh
port=ncurses
version=6.3
useconfigure=true
configopts=(
    "--enable-pc-files"
    "--enable-sigwinch"
    "--enable-term-driver"
    "--with-pkg-config=/usr/local/lib/pkgconfig"
    "--with-pkg-config-libdir=/usr/local/lib/pkgconfig"
    "--with-shared"
    "--without-ada"
    "--enable-widec"
)
files="https://invisible-mirror.net/archives/ncurses/ncurses-${version}.tar.gz ncurses-${version}.tar.gz 97fc51ac2b085d4cde31ef4d2c3122c21abc217e9090a43a30fc5ec21684e059"
auth_type="sha256"

check_tic_version() {
    local tic_path="$1"

    # "ncurses A.B.C" -> "A.B.C" -> "A"
    local major_version="$($tic_path -V | cut -d ' ' -f2 | cut -d '.' -f1)"

    [ "$major_version" -ge 6 ]
}

get_tic_path() {
    # Installation involves generating terminfo files using `tic`.
    # It seems MacOS ships with an older version of ncurses which has a
    # `tic` binary that cannot properly parse the `terminfo.src` that ships
    # with the version we build.
    # So ensure that at least tic 6.0 is available.
    # See https://github.com/termux/termux-packages/issues/4487#issuecomment-626277493
    # and https://lists.gnu.org/archive/html/bug-ncurses/2019-07/msg00020.html.

    if command -v tic >/dev/null; then
        if check_tic_version "tic"; then
            echo "tic"
            return 0
        else
            # Check for Homebrew installation.
            if command -v brew >/dev/null; then
                local cellar_path=$(brew --cellar ncurses)
                local highest_version=$(ls "$cellar_path" | head -1)
                local tic_path="$cellar_path/$highest_version/bin/tic"

                if check_tic_version "$tic_path"; then
                    echo "$tic_path"
                    return 0
                fi
            fi
        fi
    fi

    return 1
}

pre_configure() {
    export CPPFLAGS="-P"
}

install() {
    local tic_path=$(get_tic_path)
    if [ ! $? ]; then
        echo "Error: installing cross-compiled ncurses requires locally installed ncurses >= 6.0"
        exit 1
    fi

    echo "Using $tic_path from $($tic_path -V)"

    export TIC_PATH="$tic_path"
    run make DESTDIR=$DESTDIR "${installopts[@]}" install
}

post_install() {
    # Compatibility symlinks for merged libraries.
    for lib in tinfo tic curses; do
        ln -svf libncursesw.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/lib${lib}w.so"
    done

    # Compatibility symlinks for non-w libraries.
    for lib in form menu ncurses ncurses++ panel tinfo tic curses; do
        ln -svf lib${lib}w.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/lib${lib}.so"
    done

    # Compatibility symlink for the include folder.
    # Target folder has to be removed, otherwise we will get `/usr/local/include/ncurses/ncursesw`.
    rm -rf "${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
    ln -svf ncursesw "${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
}
