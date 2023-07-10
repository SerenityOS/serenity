#!/usr/bin/env -S bash ../.port_include.sh
port=nyancat
version=git
workdir=nyancat-master
files=(
    "https://github.com/klange/nyancat/archive/master.tar.gz nyancat-git.tar.gz cfd6c817f25adcecc9490321991ecb571bfdfe0d8c249663843d3df4194f935d"
)
launcher_name=Nyancat
launcher_category=Games
launcher_command=nyancat
launcher_run_in_terminal=true
