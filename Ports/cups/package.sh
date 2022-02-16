#!/usr/bin/env -S bash ../.port_include.sh

port="cups"
version="2.3.3"
workdir=cups-2.3.3
useconfigure="true"
files="https://github.com/apple/cups/releases/download/v2.3.3/cups-2.3.3-source.tar.gz cups-2.3.3-source.tar.gz"
depends=()
configopts=("--prefix=/usr/local")

pre_configure() {
    #NOCONFIGURE=1 run ./autogen.sh
    run sed -i 's@irix\* \\@irix* | *serenity* \\@' config.sub
}
