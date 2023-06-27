#!/usr/bin/env -S bash ../.port_include.sh

port="cavestory"
version="2.6.5-1"
files="https://github.com/nxengine/nxengine-evo/archive/refs/heads/master.zip master.zip d013acbf399938b5d3d10fd4d37817b630559e2b24b90576cde6f88d445ea150"
auth_type='sha256'
depends=(
  "gcc"
  "cmake"
  "SDL2"
  "SDL2_ttf"
  "libpng"
  "SDL2_mixer"
  "libjpeg"
  "SDL2_image"
)
workdir="nxengine-evo-master"
launcher_name="Cave Story"
launcher_category="Games"
launcher_command="/usr/local/bin/nxengine-evo"
icon_file="platform/switch/icon.jpg"

post_fetch () {
  mkdir -p $workdir/build
}

build () {
  cd $workdir/build; cmake -DCMAKE_BUILD_TYPE=Release .. 
  make
}

pre_install () {
  curl https://www.cavestory.org/downloads/cavestoryen.zip -O 
  unzip cavestoryen.zip
  mv CaveStory/Doukutsu.exe $workdir/
  mv CaveStory/data/* $workdir/data/
  rm cavestoryen.zip
  rm -rf CaveStory
  export PATH="$PORT_BUILD_DIR/$workdir":$PATH
  cd $workdir; nxextract  
}

install () {
  cd $workdir/build; make prefix=/usr/local DESTDIR=$SERENITY_INSTALL_ROOT install
}

uninstall () {
  cd $workdir/build; make --help
}
