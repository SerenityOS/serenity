#!/usr/bin/env -S bash ../.port_include.sh
port=doom
workdir=SerenityDOOM-master
version=serenity-git
files="https://github.com/SerenityOS/SerenityDOOM/archive/master.tar.gz doom-git.tar.gz 481406ef30e04ad55d39aa94baab73bd"
auth_type=md5
makeopts="-C doomgeneric/"
installopts="-C doomgeneric/"
launcher_name=Doom
launcher_category=Games
launcher_command=doom
