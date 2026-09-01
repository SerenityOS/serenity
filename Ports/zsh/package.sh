#!/usr/bin/env -S bash ../.port_include.sh
port='zsh'
version='5.9.2'
files=(
    "https://sourceforge.net/projects/zsh/files/zsh/${version}/zsh-${version}.tar.xz#36fa734374b44783582cec09bcd67822e2f992c779ec1624ab5596df078d2f81"
)
launcher_name='Zsh'
launcher_category='&Utilities'
launcher_command='/usr/local/bin/zsh'
launcher_run_in_terminal='true'
useconfigure=true

pre_configure() {
    run "./Util/preconfig"
}

post_configure() {
    run_replace_in_file "s/define HAVE_PRCTL 1/undef HAVE_PRCTL/" config.h
}

post_install() {
    cp "${PORT_META_DIR}/zshrc" "${SERENITY_INSTALL_ROOT}/etc/"
}
