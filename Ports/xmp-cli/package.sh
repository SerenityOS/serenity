#!/usr/bin/env -S bash ../.port_include.sh
port='xmp-cli'
version='4.2.0'
workdir="xmp-${version}"
files=(
    "https://github.com/libxmp/xmp-cli/releases/download/xmp-${version}/xmp-${version}.tar.gz#dc54513af9a4681029a1243fd0c9cdf153d813a1125de6c782926674285bc5ae"
)
useconfigure='true'
use_fresh_config_sub='false'
configopts=(
	'--disable-alsa'
	'--disable-oss'
	'--disable-sndio'
)
depends=( 'libxmp' )
