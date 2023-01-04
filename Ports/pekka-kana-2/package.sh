#!/usr/bin/env -S bash ../.port_include.sh
port=pekka-kana-2
version=1.2.7
archive_hash=07485c72173e5a3a87f55e8a749bf77526dae57cb1b34c5aa2eb0e17f7bd6f7b
files="http://deb.debian.org/debian/pool/main/p/pekka-kana-2/pekka-kana-2_${version}.orig.tar.bz2 pk2-${version}.tar.bz2 $archive_hash"
auth_type=sha256
depends=("SDL2" "SDL2_mixer" "SDL2_image")
launcher_name="Pekka Kana 2"
launcher_category="Games"
launcher_command=/usr/local/games/pekka-kana-2/pekka-kana-2
icon_file="data/gfx/pk2.ico"

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/games/pekka-kana-2"
    run cp bin/pekka-kana-2 "${SERENITY_INSTALL_ROOT}/usr/local/games/pekka-kana-2/pekka-kana-2"
    for dir in episodes gfx language music sfx sprites; do
        run cp -r data/$dir "${SERENITY_INSTALL_ROOT}/usr/local/games/pekka-kana-2/$dir"
    done
}
