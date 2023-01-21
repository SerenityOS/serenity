#!/usr/bin/env -S bash ../.port_include.sh
port='md2roff'
version='b28141f69e0d558da986f6a4e38d48f351628342'
auth_type='sha256'
files="https://github.com/nereusx/md2roff/archive/${version}.tar.gz md2roff-${version}.tar.gz 8e44184654f54cc63114e6b3eca24a62735e054e8b7cb3cbd71597114aef476f"

build() {
    run make -B md2roff
}

install() {
    run cp -f md2roff "${SERENITY_INSTALL_ROOT}/bin/md2roff"
    run chmod 0755 "${SERENITY_INSTALL_ROOT}/bin/md2roff"
}
