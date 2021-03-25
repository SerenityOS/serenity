/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "SoundPlayerWidget.h"
#include <LibAudio/ClientConnection.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/CharacterBitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd accept rpath thread unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd accept rpath thread unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto audio_client = Audio::ClientConnection::construct();
    audio_client->handshake();

    if (pledge("stdio recvfd sendfd accept rpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-sound-player");

    auto window = GUI::Window::construct();
    window->set_title("Sound Player");
    window->set_resizable(false);
    window->resize(350, 140);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("File");
    auto& player = window->set_main_widget<SoundPlayerWidget>(window, audio_client);

    if (argc > 1) {
        String path = argv[1];
        player.open_file(path);
        player.manager().play();
    }

    auto hide_scope = GUI::Action::create_checkable("Hide scope", { Mod_Ctrl, Key_H }, [&](auto& action) {
        player.hide_scope(action.is_checked());
    });

    app_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open sound file...");
        if (path.has_value()) {
            player.open_file(path.value());
        }
    }));
    app_menu.add_action(move(hide_scope));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& playback_menu = menubar->add_menu("Playback");

    auto loop = GUI::Action::create_checkable("Loop", { Mod_Ctrl, Key_R }, [&](auto& action) {
        player.manager().loop(action.is_checked());
    });

    playback_menu.add_action(move(loop));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Sound Player", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();
    return app->exec();
}
