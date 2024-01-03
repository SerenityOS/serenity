#!/usr/bin/env -S bash ../.port_include.sh
port='jfduke3d'
version='41cd46bc00633e7457d07d88c8add9f99a7d9d41'
workdir="jfduke3d-${version}"
install_dir='/usr/local/share/games/jfduke3d'
launcher_name='Duke Nukem 3D'
launcher_category='&Games'
launcher_command="${install_dir}/duke3d"
icon_file='rsrc/game_icon.ico'
depends=(
    'SDL2'
)
jfaudiolib_commit='d72aa171a6fad72d4f19e689f4be989fe472e763'
jfbuild_commit='d3f86131e1eb5fb1397c3e7d477caef675c359d2'
jfmact_commit='1f0746a3b9704906669d8aaed2bbb982053a393e'
files=(
    "https://github.com/jonof/jfduke3d/archive/${version}.tar.gz#cef20187b9e9b69b48007e9aaf0c41996aa227abccc5e2fd4ecece2cb503934d"
    "https://github.com/jonof/jfaudiolib/archive/${jfaudiolib_commit}.tar.gz#520204c06d6be41838b5a6575a9cbb5dc8889345624c74ff8b9bacee005bd819"
    "https://github.com/jonof/jfbuild/archive/${jfbuild_commit}.tar.gz#8917f91f10d595fffd3e79e2c816c9130448d8480fb804eba4a880fb0676b0b5"
    "https://github.com/jonof/jfmact/archive/${jfmact_commit}.tar.gz#173d978cbeec1b387aac458f77d831d64aa56bb30164939ba75885cea04c777d"
)
makeopts=(
    'USE_OPENGL=0'
    'WITHOUT_GTK=1'
    'USE_POLYMOST=1'
    "SDL2CONFIG=${SERENITY_BUILD_DIR}/Root/usr/local/bin/sdl2-config"
)

pre_patch() {
    pushd "${workdir}"

    # Initialize submodules from tarballs
    rm -rf jfaudiolib
    cp -r ../jfaudiolib-${jfaudiolib_commit} jfaudiolib
    rm -rf jfbuild
    cp -r ../jfbuild-${jfbuild_commit} jfbuild
    rm -rf jfmact
    cp -r ../jfmact-${jfmact_commit} jfmact

    popd
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}${install_dir}"
    run cp build "${SERENITY_INSTALL_ROOT}${install_dir}"
    run cp duke3d "${SERENITY_INSTALL_ROOT}${install_dir}"
}

post_install() {
  echo
  echo 'Duke Nukem 3D is installed!'
  echo
  echo 'Make sure your game files are present in the installation directory:'
  echo '    Inside SerenityOS: ~/.jfduke3d'
  echo "    Outside SerenityOS: ${SERENITY_INSTALL_ROOT}/Base/home/anon/.jfduke3d"
  echo
  echo 'For more information you can visit the JFDuke3D Documentation: https://www.jonof.id.au/jfduke3d/readme.html'
}
