#!/usr/bin/env -S bash ../.port_include.sh
port=libICE
version=1.0.10
useconfigure=true
files="https://www.x.org/releases/individual/lib/libICE-${version}.tar.gz libICE-${version}.tar.gz 1116bc64c772fd127a0d0c0ffa2833479905e3d3d8197740b3abd5f292f22d2d"
depends="xproto xtrans"
auth_type="sha256"
