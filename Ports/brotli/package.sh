#!/usr/bin/env -S bash ../.port_include.sh

port='brotli'
version='1.0.9'
files=(
    "https://github.com/google/brotli/archive/refs/tags/v${version}.tar.gz brotli-v${version}.tar.gz f9e8d81d0405ba66d181529af42a3354f838c939095ff99930da6aa9cdf6fe46"
)
useconfigure='true'

configure() {
    run ./configure-cmake
}
