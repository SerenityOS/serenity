#!/usr/bin/env -S bash ../.port_include.sh
port='SonicMania'
version='1.05.0713'
workdir="RSDKv5-Decompilation-1b01682d22948aaa3971e96f20f038c144b7eaf4"
launcher_name='Sonic Mania'
launcher_category='&Games'
launcher_command='/usr/local/share/games/sonicmania/RSDKv5U'
icon_file='RSDKv5U/RSDKv5U.ico'
depends=(
  'SDL2'
  'SDL2_sound'
  'SDL2_mixer'
  'libtheora'
  'libogg'
  'stb'
)
files=(
    "https://github.com/Rubberduckycooly/RSDKv5-Decompilation/archive/1b01682d22948aaa3971e96f20f038c144b7eaf4.tar.gz#b12ec54e5d763a4a48b1f4a3ca19cca4441c6efdaa95d77614dc82d1ab11b916"
    "https://github.com/Rubberduckycooly/Sonic-Mania-Decompilation/archive/refs/tags/v1.0.0-win.tar.gz#a4c6f4cca584050fb5df9b15df30b7c46d2d75105a23ccb9e78a03064ef47264"
    "https://github.com/nothings/stb/archive/5736b15f7ea0ffb08dd38af21067c314d6a3aae9.zip#b6601f182fa4bc04dd0f135e38231e8a2c6c9e7901c66a942871f03285713b05"
    "https://github.com/leethomason/tinyxml2/archive/refs/tags/10.0.0.tar.gz#3bdf15128ba16686e69bce256cc468e76c7b94ff2c7f391cc5ec09e40bff3839"
)
makeopts=(
  'PLATFORM=Serenity'
)

pre_patch() {
  run rm -rf Game
  run rm -rf dependencies/all/stb_vorbis
  run rm -rf dependencies/all/tinyxml2
  run mv ../Sonic-Mania-Decompilation-1.0.0-win/SonicMania Game
  run mv ../stb-5736b15f7ea0ffb08dd38af21067c314d6a3aae9/ dependencies/all/stb_vorbis
  run mv ../tinyxml2-10.0.0 dependencies/all/tinyxml2/
}

install() {
  run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/sonicmania/
  run cp -RT bin/Serenity/SDL2/RSDKv5U ${SERENITY_INSTALL_ROOT}/usr/local/share/games/sonicmania/
  run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/home/anon/RSDKv5
  run cp -RT bin/Serenity/SDL2/Game.so ${SERENITY_INSTALL_ROOT}/home/anon/RSDKv5
}

post_install() {
  echo
  echo 'Sonic Mania has been installed!.'
  echo
  echo 'But it wont launch properly due to a missing file called Data.rsdk'
  echo 'This is because it has not been packed and it will never be.'
  echo "Due to piracy. If you want the game to work"
  echo "You need to buy the game and get a legal Data.rsdk from your game."
  echo
  echo 'Here are the paths where you should copy the file if a legal one is acquired:'
  echo 'Inside SerenityOS: ~/RSDKv5'
  echo "Outside SerenityOS: ${SERENITY_INSTALL_ROOT}/home/anon/RSDKv5"
  echo
  echo "NOTE: If this script installed Sonic Mania PLUS"
  echo "You will then need to get a Data.rsdk from a copy of the Legal Sonic Mania PLUS!"
  echo "To verify try opening with a Sonic Mania Data.rsdk"
  echo "If that causes it to be stuck on Mods loading done."
  echo "Then this script installed Sonic Mania PLUS and need to get"
  echo "The Sonic Mania PLUS Data.rsdk"
}
