#!/bin/bash ../.port_include.sh
port='schismtracker'
version='20240523'
useconfigure='true'
files=(
    "https://github.com/schismtracker/schismtracker/releases/download/${version}/schismtracker-${version}.source.tar.gz#44b3de30ad0c72c540f60cfdfe4f3906c5756023fd17e23a0977646d425d0863"
)
depends=(
    'SDL2'
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
