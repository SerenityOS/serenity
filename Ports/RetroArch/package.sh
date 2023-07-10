#!/usr/bin/env -S bash ../.port_include.sh
port=RetroArch
useconfigure="true"
version="1.12.0"
archive_hash="c912e32a0300f16ade827d48a4a948d5dab40b764cd1169f61108c6f5803649a"
files=(
    "https://github.com/libretro/${port}/archive/refs/tags/v${version}.tar.gz ${port}-${version}.tar.gz $archive_hash"
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
launcher_category=Games
launcher_command=/usr/local/bin/retroarch
icon_file=media/retroarch.ico

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL -I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"

post_install() {
echo "==== Post installation instructions ===="
echo "Please remember to use the online updater"
echo "to install cores info files using main menu"
echo "online updater > update core info files"
echo "before installing libretro cores from the port"
}
