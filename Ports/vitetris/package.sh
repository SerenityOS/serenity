#!/usr/bin/env -S bash ../.port_include.sh
port='vitetris'
useconfigure='true'
version='0.59.1'
files=(
    "https://github.com/vicgeralds/vitetris/archive/refs/tags/v${version}.tar.gz#699443df03c8d4bf2051838c1015da72039bbbdd0ab0eede891c59c840bdf58d"
)
configopts=(
    '--without-xlib'
    '--without-joystick'
    '--without-network'
)
launcher_name='vitetris'
launcher_category='&Games'
launcher_command='/usr/local/bin/tetris'
launcher_run_in_terminal='true'

configure() {
    run chmod +x "${configscript}"
    run "./${configscript}" "${configopts[@]}"
}
