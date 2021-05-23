/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NoVisualizationWidget.h"
#include "Player.h"
#include "SampleWidget.h"
#include "SoundPlayerWidgetAdvancedView.h"
#include <LibAudio/ClientConnection.h>
#include <LibGUI/Action.h>
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

    PlaybackManager playback_manager(audio_client);
    PlayerState initial_player_state { true,
        true,
        false,
        false,
        false,
        44100,
        1.0,
        audio_client,
        playback_manager,
        "" };

    auto app_icon = GUI::Icon::default_icon("app-sound-player");

    auto window = GUI::Window::construct();
    window->set_title("Sound Player");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");

    auto& playlist_menu = menubar->add_menu("Play&list");

    String path = argv[1];
    // start in advanced view by default
    Player* player = &window->set_main_widget<SoundPlayerWidgetAdvancedView>(window, initial_player_state);
    if (argc > 1) {
        player->open_file(path);
    }

    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open sound file...");
        if (path.has_value()) {
            player->open_file(path.value());
        }
    }));

    auto linear_volume_slider = GUI::Action::create_checkable("&Nonlinear Volume Slider", [&](auto& action) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_nonlinear_volume_slider(action.is_checked());
    });
    file_menu.add_action(linear_volume_slider);

    auto playlist_toggle = GUI::Action::create_checkable("&Show Playlist", [&](auto& action) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_playlist_visible(action.is_checked());
    });
    playlist_menu.add_action(playlist_toggle);
    if (path.ends_with(".m3u") || path.ends_with(".m3u8"))
        playlist_toggle->set_checked(true);
    playlist_menu.add_separator();

    auto playlist_loop_toggle = GUI::Action::create_checkable("&Loop Playlist", [&](auto& action) {
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_looping_playlist(action.is_checked());
    });
    playlist_menu.add_action(playlist_loop_toggle);

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& playback_menu = menubar->add_menu("&Playback");

    auto loop = GUI::Action::create_checkable("&Loop", { Mod_Ctrl, Key_R }, [&](auto& action) {
        player->set_looping_file(action.is_checked());
    });

    playback_menu.add_action(move(loop));

    auto& visualization_menu = menubar->add_menu("&Visualization");
    Vector<NonnullRefPtr<GUI::Action>> visualization_checkmarks;
    GUI::Action* checked_vis = nullptr;
    auto uncheck_all_but = [&](GUI::Action& one) {for (auto& a : visualization_checkmarks) if (a != &one) a->set_checked(false); };

    auto bars = GUI::Action::create_checkable("&Bars", [&](auto& action) {
        uncheck_all_but(action);
        if (checked_vis == &action) {
            action.set_checked(true);
            return;
        }
        checked_vis = &action;
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<BarsVisualizationWidget>();
    });
    bars->set_checked(true);

    visualization_menu.add_action(bars);
    visualization_checkmarks.append(bars);

    auto samples = GUI::Action::create_checkable("&Samples", [&](auto& action) {
        uncheck_all_but(action);
        if (checked_vis == &action) {
            action.set_checked(true);
            return;
        }
        checked_vis = &action;
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<SampleWidget>();
    });

    visualization_menu.add_action(samples);
    visualization_checkmarks.append(samples);

    auto none = GUI::Action::create_checkable("&None", [&](auto& action) {
        uncheck_all_but(action);
        if (checked_vis == &action) {
            action.set_checked(true);
            return;
        }
        checked_vis = &action;
        static_cast<SoundPlayerWidgetAdvancedView*>(player)->set_visualization<NoVisualizationWidget>();
    });

    visualization_menu.add_action(none);
    visualization_checkmarks.append(none);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Sound Player", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();
    return app->exec();
}
