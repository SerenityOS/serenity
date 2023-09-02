#!/usr/bin/env -S bash ../.port_include.sh
port=SDL_mixer
version=1.2.12
useconfigure=true
configopts=("--disable-static")
use_fresh_config_sub=true
config_sub_paths=("build-scripts/config.sub")
files=(
    "https://www.libsdl.org/projects/SDL_mixer/release/SDL_mixer-${version}.tar.gz#1644308279a975799049e4826af2cfc787cad2abb11aa14562e402521f86992a"
)
depends=("libmikmod" "libvorbis" "sdl12-compat" "timidity")

# Explicitly point to the config binaries installed by our ports. Otherwise, it will
# only work if by chance your host machine has those binaries in $PATH.
export LIBMIKMOD_CONFIG="${SERENITY_INSTALL_ROOT}/usr/local/bin/libmikmod-config"
export SDL_CONFIG="${SERENITY_INSTALL_ROOT}/usr/local/bin/sdl-config"
