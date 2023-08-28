#!/usr/bin/env -S bash ../.port_include.sh
port=acpica-tools
version='R06_28_23'
workdir="acpica-${version}"
files=(
    "https://github.com/acpica/acpica/archive/refs/tags/${version}.tar.gz 2248799b7ca08a7711ac87d31924354ed49047507607d033bd327ba861ec4d31"
)

makeopts+=(
    'iasl'
    'acpixtract'
    # FIXME: Add "acpiexec" to work
    'acpihelp'
    'acpisrc'
    'acpibin'
)

if [[ "$SERENITY_TOOLCHAIN" == "GNU" ]]; then
    makeopts+=(
        CWARNINGFLAGS='-Wlogical-op -Wmissing-parameter-type -Wold-style-declaration'
    )
fi
