#!/usr/bin/env -S bash ../.port_include.sh
port=libXmu
version=1.1.3
useconfigure=true
files="https://www.x.org/releases/individual/lib/libXmu-${version}.tar.gz libXmu-${version}.tar.gz 5bd9d4ed1ceaac9ea023d86bf1c1632cd3b172dce4a193a72a94e1d9df87a62e"
depends="libXt libXext libX11 xextproto"
auth_type="sha256"
