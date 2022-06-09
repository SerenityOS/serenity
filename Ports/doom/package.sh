#!/usr/bin/env -S bash ../.port_include.sh
port='doom'
version='d5d5a91113d12fd3b75e6c1fa2e33106dd7e1c82'
files="https://github.com/ozkl/doomgeneric/archive/${version}.tar.gz doom-${version}.tar.gz c130a51a87489966e816414f6c89b9a575872bae3c8a1eaf0fbfafb6f9b81af5"
auth_type='sha256'
workdir="doomgeneric-${version}"
makeopts=("-C" "doomgeneric/")
installopts=("-C" "doomgeneric/")
launcher_name='Doom'
launcher_category='Games'
launcher_command='doom'
