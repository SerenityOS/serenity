#!/usr/bin/env -S bash ../.port_include.sh
port='libopus'
version='1.5.2'
workdir='opus-1.5.2'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://downloads.xiph.org/releases/opus/opus-${version}.tar.gz#65c1d2f78b9f2fb20082c38cbe47c951ad5839345876e46941612ee87f9a7ce1"
)
