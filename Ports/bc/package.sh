#!/usr/bin/env -S bash ../.port_include.sh
port='bc'
version='6.1.1'
files="https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz bc-${version}.tar.xz b6de9e2fa4fcb1902c1686760dd90217543128f0298d418951ab1e9a03964097"
auth_type='sha256'
useconfigure='true'
configscript='configure.sh'
configopts=("--prefix=/usr/local" "--disable-nls")

export CFLAGS='-O3 -flto'

configure() {
    run ./"${configscript}" "${configopts[@]}"
}
