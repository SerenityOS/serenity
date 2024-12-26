#!/usr/bin/env -S bash ../.port_include.sh
port='coreutils'
version='9.5'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
files=(
    "https://ftpmirror.gnu.org/gnu/coreutils/coreutils-${version}.tar.gz#767ae6a22950ec42f3ba5f7c1de79dd27800ee8e9b8642da5dedb5974a1741e5"
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
