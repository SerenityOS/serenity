#!/usr/bin/env -S bash ../.port_include.sh
port='lrzip'
version='0.651'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "http://ck.kolivas.org/apps/lrzip/lrzip-${version}.tar.xz#48bd8decb097c1596c9b3777959cd3e332819434ed77a2823e65aa436f1602f9"
)
depends=(
  'bzip2'
  'lz4'
  'lzo'
  'zlib'
)
