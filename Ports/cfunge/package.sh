#!/usr/bin/env -S bash ../.port_include.sh

port='cfunge'
version='2bc4fb27ade2a816ca9a90a6d9f6958111123fa9'
useconfigure='true'
files=(
    "https://github.com/VorpalBlade/cfunge/archive/${version}.zip#364994a890ed1083684956db576a2a5cfb94b3117bae868910d6a75111033f55"
)

configure() {
    run cmake -B build "${configopts[@]}"
}

build() {
    run make -C build "${makeopts[@]}"
}

install() {
    run cp build/cfunge "${SERENITY_INSTALL_ROOT}/bin"
}
