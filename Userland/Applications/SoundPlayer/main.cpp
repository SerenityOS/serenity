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
#include <LibAudio/ClientConnection.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/CharacterBitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath thread unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto audio_client = Audio::ClientConnection::construct();

    if (pledge("stdio recvfd sendfd rpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-sound-player");

    auto window = GUI::Window::construct();
    window->set_title("Sound Player");
    window->set_icon(app_icon.bitmap_for_size(16));

    String path = argv[1];
    // start in advanced view by default
    Player* player = &window->set_main_widget<SoundPlayerWidgetAdvancedView>(window, audio_client);
    if (argc > 1) {
        player->play_file_path(path);
    }

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open sound file...");
        if (path.has_value()) {
            player->play_file_path(path.value());
        }
    }));

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& playback_menu = window->add_menu("&Playback");
    GUI::ActionGroup loop_actions;
    loop_actions.set_exclusive(true);
    auto loop_none = GUI::Action::create_checkable("&No Loop", [&](auto&) {
        player->set_loop_mode(Player::LoopMode::None);
    });
    loop_none->set_checked(true);
    loop_actions.add_action(loop_none);
    playback_menu.add_action(loop_none);

    auto loop_file = GUI::Action::create_checkable("Loop &File", { Mod_Ctrl, Key_F }, [&](auto&) {
        player->set_loop_mode(Player::LoopMode::File);
    });
    loop_actions.add_action(loop_file);
    playback_menu.add_action(loop_file);

    auto loop_playlist = GUI::Action::create_checkable("Loop &Playlist", { Mod_Ctrl, Key_P }, [&](auto&) {
        player->set_loop_mode(Player::LoopMode::Playlist);
    });
    loop_actions.add_action(loop_playlist);
    playback_menu.add_action(loop_playlist);

    auto linear_volume_slider = GUI::Action::create_checkable("&Nonlinear Volume Slider", [&](auto& action) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_nonlinear_volume_slider(action.is_checked());
    });
    playback_menu.add_separator();
    playback_menu.add_action(linear_volume_slider);
    playback_menu.add_separator();

    auto playlist_toggle = GUI::Action::create_checkable("&Show Playlist", [&](auto& action) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_playlist_visible(action.is_checked());
    });
    if (path.ends_with(".m3u") || path.ends_with(".m3u8"))
        playlist_toggle->set_checked(true);
    playback_menu.add_action(playlist_toggle);

    auto& visualization_menu = window->add_menu("&Visualization");
    GUI::ActionGroup visualization_actions;
    visualization_actions.set_exclusive(true);

    auto bars = GUI::Action::create_checkable("&Bars", [&](auto&) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<BarsVisualizationWidget>();
    });
    bars->set_checked(true);
    visualization_menu.add_action(bars);
    visualization_actions.add_action(bars);

    auto samples = GUI::Action::create_checkable("&Samples", [&](auto&) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<SampleWidget>();
    });
    visualization_menu.add_action(samples);
    visualization_actions.add_action(samples);

    auto none = GUI::Action::create_checkable("&None", [&](auto&) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<NoVisualizationWidget>();
    });
    visualization_menu.add_action(none);
    visualization_actions.add_action(none);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Sound Player", app_icon, window));

    window->show();
    return app->exec();
}
