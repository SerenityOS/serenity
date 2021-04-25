#!/usr/bin/env -S bash ../.port_include.sh
port=doom
workdir=SerenityDOOM-master
version=git
files="https://github.com/SerenityOS/SerenityDOOM/archive/master.tar.gz doom-git.tar.gz 13aaa5b1655b85190b744905c9a8a69f1a1e26e7331c9363a241e83510f5b26b"
auth_type=sha256
makeopts="-C doomgeneric/"
installopts="-C doomgeneric/"
launcher_name=Doom
launcher_category=Games
launcher_command=doom
