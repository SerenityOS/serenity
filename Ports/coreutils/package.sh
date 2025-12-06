#!/usr/bin/env -S bash ../.port_include.sh
port='coreutils'
version='9.9'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/coreutils/coreutils-${version}.tar.gz#91a719fcf923de686016f2c8d084a8be1f793f34173861273c4668f7c65af94a"
)

# Exclude some non-working utilities:
#  - arch, coreutils, and hostname are already excluded in the default configuration
#  - chcon and runcon are SELinux utilities
#  - df requires one of the read_file_system_list implementations in gnulib/lib/mountlist.c
#  - pinky, users, and who require utmp
#  - nice is just something that doesn't exist
configopts+=(
    '--enable-no-install-program=arch,coreutils,hostname,chcon,runcon,df,pinky,users,who,nice'
)
