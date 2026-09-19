#!/usr/bin/env -S bash ../.port_include.sh
port='coreutils'
version='9.12'
useconfigure='true'
files=(
    "mirror://gnu/coreutils/coreutils-${version}.tar.gz#14cbf5a4de0c7b7fa3b9fa7fada4c58b2defe33336aa8fd83d7622c5c4ebdc13"
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
