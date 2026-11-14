#!/usr/bin/env -S bash ../.port_include.sh
port='schismtracker'
version='20261102'
useconfigure='true'
files=(
    "https://github.com/schismtracker/schismtracker/releases/download/${version}/schismtracker-${version}.source.tar.gz#bb2109f688fb534a8f8efef7ce9dc374d850b45f2b25e6ebf4d9e649aabeafd0"
)
depends=(
    'SDL2'
    'utf8proc'
)
launcher_name='SchismTracker'
launcher_category='&Media'
launcher_command='/usr/local/bin/schismtracker'
configopts=(
    '--without-flac'
)

install() {
	run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
	run cp schismtracker "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
