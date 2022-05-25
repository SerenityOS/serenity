#!/usr/bin/env -S bash ../.port_include.sh

port=x264
version=bfc87b7a330f75f5c9a21e56081e4b20344f139e
useconfigure=true
files="https://code.videolan.org/videolan/x264/-/archive/${version}/x264-master.tar.gz libx264-${version}.tar.gz 2ca2344fc2d657150599687e128dac8816b07e8d5c991fa2281e51fe647bbbe7"
auth_type=sha256
configopts=("--enable-shared")
use_fresh_config_sub=true
