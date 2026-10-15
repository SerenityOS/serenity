#!/usr/bin/env -S bash ../.port_include.sh
port=RetroArch
useconfigure="true"
version="1.22.2"
files=(
    "https://github.com/libretro/${port}/archive/refs/tags/v${version}.tar.gz#245ef18c8fa8fbd9fbb5eb25cf43e17c6aace2f95c1ed99873cbd794012bb232"
)
depends=("freetype" "SDL2" "zlib")

configopts=(
    "--disable-builtinglslang"
    "--disable-discord"
    "--disable-glsl"
    "--disable-glslang"
    "--disable-opengl"
    "--disable-slang"
    "--disable-spirv_cross"
    "--disable-systemmbedtls"
    "--disable-update_cores"
)

launcher_name=RetroArch
launcher_category='&Games'
launcher_command=/usr/local/bin/retroarch
icon_file=media/retroarch.ico

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
# We don't have '<arch>-serenity-pkg-config', so just use the "normal" one.
export PKG_CONF_PATH='pkg-config'

post_install() {
echo "==== Post installation instructions ===="
echo "Please remember to use the online updater"
echo "to install cores info files using main menu"
echo "online updater > update core info files"
echo "before installing libretro cores from the port"
}
