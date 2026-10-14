#!/usr/bin/env -S bash ../.port_include.sh
port='expat'
version='2.9.0'
versionpath='2_9_0'
useconfigure='true'
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${versionpath}/expat-${version}.tar.xz#1e6371862cc31999b368c3b89b49994f0677e1bab5f1b2b85ae3741f5d803051"
)
