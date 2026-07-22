#!/usr/bin/env -S bash ../.port_include.sh
port='coreutils'
version='9.11'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/coreutils/coreutils-${version}.tar.gz#2033b8a3049c06bff49a9e3cea72bdf4683bcd0cbeb975211dd56dbaf8b736ae"
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
