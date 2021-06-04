#!/usr/bin/env -S bash ../.port_include.sh
port=scummvm
useconfigure="true"
version="2.2.0"
files="https://downloads.scummvm.org/frs/scummvm/${version}/scummvm-${version}.tar.gz scummvm-${version}.tar.gz 6ec5bd63b73861c10ca9869f27a74989a9ad6013bad30a1ef70de6ec146c2cb5"
auth_type=sha256
depends="libtheora SDL2"
configopts="
    --enable-c++11
    --enable-release-mode
    --enable-optimizations
    --opengl-mode=none
    --with-sdl-prefix=${SERENITY_BUILD_DIR}/Root/usr/local
"
launcher_name=ScummVM
launcher_category=Games
launcher_command=/usr/local/bin/scummvm

post_install() {
    if command -v convert >/dev/null; then
        run convert "icons/scummvm.ico[0]" app-32x32.png
        run convert "icons/scummvm.ico[1]" app-16x16.png
        target_binary="${SERENITY_INSTALL_ROOT}/usr/local/bin/scummvm"
        run objcopy --add-section serenity_icon_s="app-16x16.png" "${target_binary}"
        run objcopy --add-section serenity_icon_m="app-32x32.png" "${target_binary}"
    fi
}
