#!/usr/bin/env -S bash ../.port_include.sh
port='schismtracker'
version='20260524'
useconfigure='true'
files=(
    "https://github.com/schismtracker/schismtracker/releases/download/${version}/schismtracker-${version}.source.tar.gz#3b211da6ea8f1db966bfc1c85e1774bda6420c5b5d24dadea9e9463fd9f66a18"
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
