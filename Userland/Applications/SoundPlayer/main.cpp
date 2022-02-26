/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BarsVisualizationWidget.h"
#include "NoVisualizationWidget.h"
#include "Player.h"
#include "SampleWidget.h"
#include "SoundPlayerWidgetAdvancedView.h"
#include <LibAudio/ConnectionFromClient.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath thread unix"));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto audio_client = TRY(Audio::ConnectionFromClient::try_create());

    TRY(Core::System::pledge("stdio recvfd sendfd rpath thread"));

    auto app_icon = GUI::Icon::default_icon("app-sound-player");

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Sound Player");
    window->set_icon(app_icon.bitmap_for_size(16));

    // start in advanced view by default
    Player* player = TRY(window->try_set_main_widget<SoundPlayerWidgetAdvancedView>(window, audio_client));
    if (arguments.argc > 1) {
        StringView path = arguments.strings[1];
        player->play_file_path(path);
        if (player->is_playlist(path))
            player->set_loop_mode(Player::LoopMode::Playlist);
    }

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open sound file...");
        if (path.has_value()) {
            player->play_file_path(path.value());
        }
    })));

    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    })));

    auto playback_menu = TRY(window->try_add_menu("&Playback"));
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
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_nonlinear_volume_slider(action.is_checked());
    });
    TRY(playback_menu->try_add_separator());
    TRY(playback_menu->try_add_action(linear_volume_slider));
    TRY(playback_menu->try_add_separator());

    auto playlist_toggle = GUI::Action::create_checkable("&Show Playlist", [&](auto& action) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_playlist_visible(action.is_checked());
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

    auto visualization_menu = TRY(window->try_add_menu("&Visualization"));
    GUI::ActionGroup visualization_actions;
    visualization_actions.set_exclusive(true);

    auto bars = GUI::Action::create_checkable("&Bars", [&](auto&) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<BarsVisualizationWidget>();
    });
    bars->set_checked(true);
    TRY(visualization_menu->try_add_action(bars));
    visualization_actions.add_action(bars);

    auto samples = GUI::Action::create_checkable("&Samples", [&](auto&) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<SampleWidget>();
    });
    TRY(visualization_menu->try_add_action(samples));
    visualization_actions.add_action(samples);

    auto none = GUI::Action::create_checkable("&None", [&](auto&) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<NoVisualizationWidget>();
    });
    TRY(visualization_menu->try_add_action(none));
    visualization_actions.add_action(none);

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Sound Player", app_icon, window)));

    window->show();
    return app->exec();
}
