#!/usr/bin/env -S bash ../.port_include.sh
port='potrace'
description='Bitmap tracing utility'
version='1.16'
website='https://potrace.sourceforge.net/'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
files="https://potrace.sourceforge.net/download/${version}/potrace-${version}.tar.gz potrace-${version}.tar.gz be8248a17dedd6ccbaab2fcc45835bb0502d062e40fbded3bc56028ce5eb7acc"
auth_type='sha256'
configopts=(
    "--with-libpotrace"
)
