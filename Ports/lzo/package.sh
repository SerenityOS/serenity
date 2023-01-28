#!/usr/bin/env -S bash ../.port_include.sh
port='lzo'
description='LZO lossless data compression algorithm'
version='2.10'
website='https://www.oberhumer.com/opensource/lzo/'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('autoconf/config.sub')
files="https://www.oberhumer.com/opensource/lzo/download/lzo-${version}.tar.gz lzo-${version}.tar.gz c0f892943208266f9b6543b3ae308fab6284c5c90e627931446fb49b4221a072"
auth_type='sha256'
