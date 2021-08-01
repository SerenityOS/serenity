#!/usr/bin/env -S bash ../.port_include.sh
port=libSM
version=1.2.3
useconfigure=true
files="https://www.x.org/releases/individual/lib/libSM-${version}.tar.gz libSM-${version}.tar.gz 1e92408417cb6c6c477a8a6104291001a40b3bb56a4a60608fdd9cd2c5a0f320"
depends="libICE xproto xtrans"
auth_type="sha256"
