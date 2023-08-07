#!/usr/bin/env -S bash ../.port_include.sh
port='bison'
version='3.8'
useconfigure='true'
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local")
files=(
    "https://ftpmirror.gnu.org/gnu/bison/bison-${version}.tar.gz d5d184d421aee15603939973a6b0f372f908edfb24c5bc740697497021ad9458"
)
