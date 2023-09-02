#!/usr/bin/env -S bash ../.port_include.sh

port='optipng'
version='0.7.7'
files=(
    "http://downloads.sourceforge.net/optipng/optipng-${version}.tar.gz#4f32f233cef870b3f95d3ad6428bfe4224ef34908f1b42b0badf858216654452"
)
useconfigure='true'

configure() {
    run ./configure
}
