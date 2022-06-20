#!/usr/bin/env -S bash ../.port_include.sh
port='bc'
version='5.2.5'
files="https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz bc-${version}.tar.xz 085d1f0d544f34c6e186a4b91e8978702eaa7645e39c630184efd49f17b3dbd5"
auth_type='sha256'
useconfigure='true'
configscript='configure.sh'
configopts=("--prefix=/usr/local" "--disable-nls")

export CFLAGS='-O3 -flto'

configure() {
    run ./"${configscript}" "${configopts[@]}"
}
