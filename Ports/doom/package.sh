#!/usr/bin/env -S bash ../.port_include.sh
port=doom
workdir=SerenityDOOM-master
description='DOOM'
version=git
website='https://github.com/SerenityOS/SerenityDOOM'
files="https://github.com/SerenityPorts/SerenityDOOM/archive/master.tar.gz doom-git.tar.gz"
makeopts=("-C" "doomgeneric/")
installopts=("-C" "doomgeneric/")
launcher_name=Doom
launcher_category=Games
launcher_command=doom
