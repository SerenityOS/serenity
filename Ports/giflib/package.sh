#!/usr/bin/env -S bash ../.port_include.sh
port='giflib'
version='6.1.3'
files=(
    "https://downloads.sourceforge.net/project/giflib/giflib-6.x/giflib-${version}.tar.gz#b65b66b99f0424b93525f987386f22fc5efb9da2bfc92ad4a532249aaffbab0e"
)
makeopts+=('UNAME=SerenityOS')
installopts+=('UNAME=SerenityOS')
