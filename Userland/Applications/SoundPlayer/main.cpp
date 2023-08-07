/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AlbumCoverVisualizationWidget.h"
#include "BarsVisualizationWidget.h"
#include "Player.h"
#include "SampleWidget.h"
#include "SoundPlayerWidget.h"
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/FlacLoader.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibImageDecoderClient/Client.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath thread unix proc"));

    StringView file_path;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(file_path, "Path to audio file to play", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));
    auto audio_client = TRY(Audio::ConnectionToServer::try_create());
    auto decoder_client = TRY(ImageDecoderClient::Client::try_create());

    Config::pledge_domains({ "SoundPlayer", "FileManager" });
    app->set_config_domain("SoundPlayer"_string);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath thread proc"));

    auto app_icon = GUI::Icon::default_icon("app-sound-player"sv);

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Sound Player");
    window->set_icon(app_icon.bitmap_for_size(16));

    // start in advanced view by default
    Player* player = TRY(window->set_main_widget<SoundPlayerWidget>(window, audio_client, decoder_client));

    if (!file_path.is_empty()) {
        player->play_file_path(file_path);
        if (player->is_playlist(file_path))
            player->set_loop_mode(Player::LoopMode::Playlist);
    }

    auto file_menu = TRY(window->try_add_menu("&File"_short_string));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<DeprecatedString> path = GUI::FilePicker::get_open_filepath(window);
        if (path.has_value()) {
            player->play_file_path(path.value());
        }
    })));

    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    })));

    auto playback_menu = TRY(window->try_add_menu("&Playback"_string));
    GUI::ActionGroup loop_actions;
    loop_actions.set_exclusive(true);
    auto loop_none = GUI::Action::create_checkable("&No Loop", { Mod_Ctrl, Key_N }, [&](auto&) {
        player->set_loop_mode(Player::LoopMode::None);
    });
    loop_actions.add_action(loop_none);
    TRY(playback_menu->try_add_action(loop_none));

    auto loop_file = GUI::Action::create_checkable("Loop &File", { Mod_Ctrl, Key_F }, [&](auto&) {
        player->set_loop_mode(Player::LoopMode::File);
    });
    loop_actions.add_action(loop_file);
    TRY(playback_menu->try_add_action(loop_file));

    auto loop_playlist = GUI::Action::create_checkable("Loop &Playlist", { Mod_Ctrl, Key_P }, [&](auto&) {
        player->set_loop_mode(Player::LoopMode::Playlist);
    });
    loop_actions.add_action(loop_playlist);
    playback_menu->add_action(loop_playlist);

    auto linear_volume_slider = GUI::Action::create_checkable("&Nonlinear Volume Slider", [&](auto& action) {
        static_cast<SoundPlayerWidget*>(player)->set_nonlinear_volume_slider(action.is_checked());
    });
    TRY(playback_menu->try_add_separator());
    TRY(playback_menu->try_add_action(linear_volume_slider));
    TRY(playback_menu->try_add_separator());

    auto playlist_toggle = GUI::Action::create_checkable("&Show Playlist", [&](auto& action) {
        static_cast<SoundPlayerWidget*>(player)->set_playlist_visible(action.is_checked());
    });
    if (player->loop_mode() == Player::LoopMode::Playlist) {
        playlist_toggle->set_checked(true);
        loop_playlist->set_checked(true);
    } else {
        loop_none->set_checked(true);
    }
    TRY(playback_menu->try_add_action(playlist_toggle));

    auto shuffle_mode = GUI::Action::create_checkable("S&huffle Playlist", [&](auto& action) {
        if (action.is_checked())
            player->set_shuffle_mode(Player::ShuffleMode::Shuffling);
        else
            player->set_shuffle_mode(Player::ShuffleMode::None);
    });
    TRY(playback_menu->try_add_action(shuffle_mode));

    auto visualization_menu = TRY(window->try_add_menu("&Visualization"_string));
    GUI::ActionGroup visualization_actions;
    visualization_actions.set_exclusive(true);

    auto set_selected_visualization_in_config = [](StringView name) {
        Config::write_string("SoundPlayer"sv, "Preferences"sv, "Visualization"sv, name);
    };

    auto bars = GUI::Action::create_checkable("&Bars", [&](auto&) {
        static_cast<SoundPlayerWidget*>(player)->set_visualization<BarsVisualizationWidget>();
        set_selected_visualization_in_config("bars"sv);
    });
    TRY(visualization_menu->try_add_action(bars));
    visualization_actions.add_action(bars);

    auto samples = GUI::Action::create_checkable("&Samples", [&](auto&) {
        static_cast<SoundPlayerWidget*>(player)->set_visualization<SampleWidget>();
        set_selected_visualization_in_config("samples"sv);
    });
    TRY(visualization_menu->try_add_action(samples));
    visualization_actions.add_action(samples);

    auto album_cover_visualization = GUI::Action::create_checkable("&Album Cover", [&](auto&) {
        auto* view = static_cast<SoundPlayerWidget*>(player);
        view->set_visualization<AlbumCoverVisualizationWidget>([&view]() {
            return view->get_image_from_music_file();
        });

        set_selected_visualization_in_config("album_cover"sv);
    });
    TRY(visualization_menu->try_add_action(album_cover_visualization));
    visualization_actions.add_action(album_cover_visualization);

    auto selected_visualization_widget = bars;
    auto visualization = Config::read_string("SoundPlayer"sv, "Preferences"sv, "Visualization"sv, "bars"sv);

    if (visualization == "samples")
        selected_visualization_widget = samples;
    else if (visualization == "album_cover")
        selected_visualization_widget = album_cover_visualization;

    selected_visualization_widget->set_checked(true);

    auto help_menu = TRY(window->try_add_menu("&Help"_short_string));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Sound Player", app_icon, window)));

    window->show();
    return app->exec();
}
