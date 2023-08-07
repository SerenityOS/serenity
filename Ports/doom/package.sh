#!/usr/bin/env -S bash ../.port_include.sh
port='doom'
commit_hash='613f870b6fa83ede448a247de5a2571092fa729d'
workdir="doomgeneric-${commit_hash}"
version='git'
files=(
    "https://github.com/ozkl/doomgeneric/archive/${commit_hash}.tar.gz fabc3e41aca92f58dfdd284754891c17875ec8c995948b49396ead6bc05b8676"
)
depends=(
    'SDL2'
    'SDL2_mixer'
)
makeopts=(
    '--directory=doomgeneric/'
    '--file=Makefile.sdl'
)
installopts=(
    '--directory=doomgeneric/'
    '--file=Makefile.sdl'
)
launcher_name='Doom'
launcher_category='Games'
launcher_command='/usr/local/bin/doom'
