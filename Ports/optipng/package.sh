#!/usr/bin/env -S bash ../.port_include.sh

port='optipng'
version='7.9.1'
files=(
    "https://downloads.sourceforge.net/project/optipng/OptiPNG/optipng-${version}/optipng-${version}.tar.gz#c2579be58c2c66dae9d63154edcb3d427fef64cb00ec0aff079c9d156ec46f29"
)
useconfigure='true'

configure() {
    run ./configure
}
