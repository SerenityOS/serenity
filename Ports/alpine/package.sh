#!/usr/bin/env -S bash ../.port_include.sh
port='alpine'
version='2.26'
useconfigure='true'
configopts=(
    '--disable-static'
    '--enable-shared'
)
use_fresh_config_sub='true'
use_fresh_config_guess='true'
files=(
    "https://alpineapp.email/alpine/release/src/alpine-2.26.tar.xz#c0779c2be6c47d30554854a3e14ef5e36539502b331068851329275898a9baba"
)
depends=(
    'openssl'
    'ncurses'
    'ca-certificates'
)
launcher_name='Alpine'
launcher_category='&Internet'
launcher_command='/usr/local/bin/alpine'
launcher_run_in_terminal='true'
icon_file='web/cgi/favicon.ico'

configure() {
    run ./"$configscript" \
        "${configopts[@]}" \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$($workdir/config.guess)" \
        --with-ssl-lib-dir="${SERENITY_INSTALL_ROOT}/usr/local/lib" \
        --with-include-path="${SERENITY_INSTALL_ROOT}/usr/local/include:${SERENITY_INSTALL_ROOT}/usr/local/include/openssl" \
        --with-system-pinerc='/etc/pine.conf' \
        --with-system-fixed-pinerc='/etc/pine.conf.fixed'
}

install() {
    run make DESTDIR="$DESTDIR" "${installopts[@]}" install
    # Install system configuration file that tells Alpine where to find CA certificates
    # installed from the `ca-certificates` package.
    # (The full `/usr/bin/install` path is used because `install()` is a function within this script.)
    run /usr/bin/install \
        --mode=644 \
        --no-target-directory \
        -D \
        pine-system.serenity.conf "${SERENITY_INSTALL_ROOT}/etc/pine.conf"
}

post_install() {
    echo "==== Post Installation Instructions ===="
    echo "To save account passwords, create an empty file named .alpine.pwd"
    echo "in your home diretory:"
    echo "    touch ~/.alpine.pwd"
}
