#!/usr/bin/env -S bash ../.port_include.sh
port='ObjFW'
version='8d19ba9c8f1955673569e10919025624975e896f'
useconfigure='true'
files=(
    "https://github.com/ObjFW/ObjFW/archive/${version}.tar.gz#4fbdeba8f2792f5fcdb75d3f0da12f927ea3b0ec0c0f16bef9a743b476f84724"
)
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'openssl'
)

# Only required for non-releases.
pre_configure() {
    run ./autogen.sh
}
