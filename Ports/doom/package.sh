#!/bin/sh ../.port_include.sh
port=doom
workdir=SerenityDOOM-master
version=serenity-git
curlopts="-L"
files="https://github.com/SerenityOS/SerenityDOOM/archive/master.tar.gz doom-git.tar.gz"
makeopts="-C doomgeneric/"
installopts="-C doomgeneric/"
