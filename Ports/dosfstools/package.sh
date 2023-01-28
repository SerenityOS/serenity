#!/usr/bin/env -S bash ../.port_include.sh
port='dosfstools'
description='dosfstools utility suite'
version='4.2'
website='https://github.com/dosfstools/dosfstools/'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
files="https://github.com/dosfstools/dosfstools/releases/download/v${version}/dosfstools-${version}.tar.gz dosfstools-${version}.tar.gz 64926eebf90092dca21b14259a5301b7b98e7b1943e8a201c7d726084809b527"
auth_type='sha256'
configopts=(
    "--enable-compat-symlinks"
)
