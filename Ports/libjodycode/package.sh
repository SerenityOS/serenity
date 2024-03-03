#!/usr/bin/env -S bash ../.port_include.sh
port='libjodycode'
version='3.1'
files=(
    "https://codeberg.org/jbruchon/libjodycode/archive/v${version}.tar.gz#837b660c305215f0cbd68b002e831f29fe1a1d823cace74d75203111eb433bbd"
)
auth_type='sha256'
workdir='libjodycode'
makeopts=("UNAME_S=serenity UNAME_M=${SERENITY_ARCH} CROSS_DETECT=cross")
