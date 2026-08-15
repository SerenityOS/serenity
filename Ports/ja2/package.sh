#!/usr/bin/env -S bash ../.port_include.sh
port='ja2'
version='0.15.1'
depends=(
    'SDL2'
)
workdir="ja2-stracciatella-${version}"
files=(
    "https://github.com/ja2-stracciatella/ja2-stracciatella/archive/refs/tags/v${version}.zip#2421d0650dac5636d84d659c8ce2b651ed12bedf149ee69e601da1c619fcd930"
)
makeopts+=(
    'SERENITY=1'
)
launcher_name='Jagged Alliance 2'
launcher_category='&Games'
launcher_command='/opt/ja2/ja2'
icon_file='Build/Res/jagged3.ico'

install() {
    installdir="${SERENITY_INSTALL_ROOT}/opt/ja2"
    run mkdir -p "${installdir}"
    run cp -r ja2 mods externalized "${installdir}"
    echo "INFO: Jagged Alliance 2 data have to be provided! Copy DATA directory located in the original Jagged Alliance 2 installation into the '${installdir}'."
    echo "INFO: Boot up SerenityOS and run '/opt/ja2/ja2' executable in order to produce ja2.ini configuration file in the '/home/anon/.ja2'."
    echo "INFO: Edit the configuration file and set 'data_dir' value to '/opt/ja2'."
}
