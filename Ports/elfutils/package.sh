#!/usr/bin/env -S bash ../.port_include.sh
port='elfutils'
version='0.186'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config/config.sub')
files="http://archive.ubuntu.com/ubuntu/pool/main/e/elfutils/elfutils_${version}.orig.tar.bz2 elfutils_${version}.orig.tar.bz2 7f6fb9149b1673d38d9178a0d3e0fb8a1ec4f53a9f4c2ff89469609879641177"
auth_type='sha256'
depends=('zlib' 'libfts' 'argp-standalone' 'musl-obstack' 'curl' 'libarchive' 'sqlite' 'gettext')
configopts=(
    '--disable-debuginfod'
)
