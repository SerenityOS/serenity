#!/usr/bin/env -S bash ../.port_include.sh
port='dos2unix'
version='7.5.7'
files=(
    "https://downloads.sourceforge.net/project/dos2unix/dos2unix/${version}/dos2unix-${version}.tar.gz#669ee27120ae71589f638fe3a167d6ea54f8633f5ab1b282551bd7a7c9510dfa"
)
depends=("gettext")
