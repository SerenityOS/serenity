#!/usr/bin/env -S bash ../.port_include.sh
port='lrzip'
version='0.651'
useconfigure='true'
files="https://github.com/ckolivas/lrzip/archive/refs/tags/v${version}.tar.gz v${version}.tar.gz f4c84de778a059123040681fd47c17565fcc4fec0ccc68fcf32d97fad16cd892"
auth_type='sha256'
depends=(
  'bzip2'
  'lz4'
  'lzo'
  'zlib'
)

configure() {
    run ./autogen.sh --host="${SERENITY_ARCH}-pc-serenity"
}
