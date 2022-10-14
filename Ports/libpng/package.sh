#!/usr/bin/env -S bash ../.port_include.sh
port=libpng
version=1.6.37
useconfigure=true
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub=true
files="https://download.sourceforge.net/libpng/libpng-${version}.tar.gz libpng-${version}.tar.gz daeb2620d829575513e35fecc83f0d3791a620b9b93d800b763542ece9390fb4"
auth_type=sha256
depends=("zlib")
