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
#include <LibGfx/CharacterBitmap.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto audio_client = Audio::ClientConnection::construct();
    audio_client->handshake();

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("SoundPlayer");
    window->set_resizable(false);
    window->set_rect(300, 300, 350, 140);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-sound-player.png"));

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("SoundPlayer");
    auto player = SoundPlayerWidget::construct(window, audio_client);

    if (argc > 1) {
        String path = argv[1];
        player->open_file(path);
        player->manager().play();
    }

    auto hide_scope = GUI::Action::create("Hide scope", { Mod_Ctrl, Key_H }, [&](GUI::Action& action) {
        action.set_checked(!action.is_checked());
        player->hide_scope(action.is_checked());
    });
    hide_scope->set_checkable(true);

    app_menu->add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath("Open wav file...");
        if (path.has_value()) {
            player->open_file(path.value());
        }
    }));
    app_menu->add_action(move(hide_scope));
    app_menu->add_separator();
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [](auto&) {
        GUI::AboutDialog::show("SoundPlayer", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-sound-player.png"));
    }));

    menubar->add_menu(move(app_menu));
    menubar->add_menu(move(help_menu));
    app.set_menubar(move(menubar));

    window->set_main_widget(player);
    window->show();

    return app.exec();
}
