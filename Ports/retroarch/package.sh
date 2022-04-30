#!/usr/bin/env -S bash ../.port_include.sh
port=retroarch
useconfigure="true"
version="1.10.3"
workdir="RetroArch-$version"
archive_hash="2af44294e55f5636262284d650cb5fff55c9070ac3a700d4fa55c1f152dcb3f2"
files="https://github.com/libretro/RetroArch/archive/refs/tags/v${version}.tar.gz retroarch-${version}.tar.gz $archive_hash"
auth_type=sha256
depends=("freetype" "SDL2" "zlib")

configopts=(
    "--disable-builtinglslang"
    "--disable-discord"
    "--disable-glsl"
    "--disable-glslang"
    "--disable-opengl"
    "--disable-slang"
    "--disable-spirv_cross"
    "--disable-update_cores"
)

launcher_name=RetroArch
launcher_category=Games
launcher_command=/usr/local/bin/retroarch
icon_file=media/retroarch.ico

function pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL -I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
}

function post_configure() {
    unset CFLAGS
}

post_install() {
    # Let'set some default options for SerenityOS
	cat <<- 'EOF' > ${SERENITY_INSTALL_ROOT}/etc/retroarch.cfg
assets_directory = "~/.config/retroarch/assets"
audio_driver = "sdl2"
audio_filter_dir = "~/.config/retroarch/filters/audio"
cheat_database_path = "~/.config/retroarch/cheats"
content_database_path = "~/.config/retroarch/database/rdb"
content_favorites_path = "~/.config/retroarch/content_favorites.lpl"
content_history_path = "~/.config/retroarch/content_history.lpl"
content_image_history_path = "~/.config/retroarch/content_image_history.lpl"
content_music_history_path = "~/.config/retroarch/content_music_history.lpl"
content_video_history_path = "~/.config/retroarch/content_video_history.lpl"
core_assets_directory = "~/.config/retroarch/downloads"
cursor_directory = "~/.config/retroarch/database/cursors"
input_remapping_directory = "~/.config/retroarch/config/remaps"
joypad_autoconfig_dir = "~/.config/retroarch/autoconfig"
libretro_directory = "/usr/lib/libretro"
libretro_info_path = "~/.config/retroarch/cores"
log_dir = "~/.config/retroarch/logs"
overlay_directory = "~/.config/retroarch/overlay"
playlist_directory = "~/.config/retroarch/playlists"
recording_config_directory = "~/.config/retroarch/records_config"
recording_output_directory = "~/.config/retroarch/records"
rgui_config_directory = "~/.config/retroarch/config"
rgui_browser_directory = "~/.config/retroarch/downloads"
savefile_directory = "~/.config/retroarch/saves"
savestate_directory = "~/.config/retroarch/states"
screenshot_directory = "~/.config/retroarch/screenshots"
system_directory = "~/.config/retroarch/system"
thumbnails_directory = "~/.config/retroarch/thumbnails"
video_filter_dir = "~/.config/retroarch/filters/video"
video_layout_directory = "~/.config/retroarch/layouts"
video_shader_dir = "~/.config/retroarch/shaders"
menu_pause_libretro = "true"
pause_nonactive = "false"
video_driver = "sdl2"
video_vsync = "false"
EOF
echo ""
echo ""
echo "==== Post installation instructions ===="
echo "Please remember to use the Online updater"
echo "to install cores info, assets, contents..."
echo "before installing libretro cores from the port"
}
