/*
 * Copyright (c) 2020, Dominic Szablewski <dominic@phoboslab.org>
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

#include "VideoPlayer.h"

#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(true);
    window->set_title("VideoPlayer");
    window->set_resizable(true);
    window->set_rect(100, 100, 320, 240+27);

    auto& player = window->set_main_widget<VideoPlayer>();

    UNUSED_PARAM(player);

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-videoplayer.png"));


    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("VideoPlayer");
    app_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath("Open mpg file...");
        if (path.has_value()) {
            player.open_file(path.value());
        }
    }));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));

    auto& view_menu = menubar->add_menu("View");
    auto full_sceen_action = GUI::CommonActions::make_fullscreen_action(
        [&](auto&) {
            window->set_fullscreen(!window->is_fullscreen());
            player.fullscreen(window->is_fullscreen());
        });
    view_menu.add_action(full_sceen_action);
    view_menu.add_separator();
    auto keep_aspect_ratio = GUI::Action::create_checkable("Keep aspect ratio", { Mod_Ctrl, Key_A }, [&](auto& action) {
        player.keep_aspect_ratio(action.is_checked());
    });
    keep_aspect_ratio->set_checked(true);
    view_menu.add_action(keep_aspect_ratio);
    view_menu.add_separator();
    view_menu.add_action(GUI::Action::create("0.5x size", { Mod_Ctrl, Key_5 }, [&](auto&) {
        player.zoom(0.5);
    }));
    view_menu.add_action(GUI::Action::create("1x size", { Mod_Ctrl, Key_1 }, [&](auto&) {
        player.zoom(1);
    }));
    view_menu.add_action(GUI::Action::create("2x size", { Mod_Ctrl, Key_2 }, [&](auto&) {
        player.zoom(2);
    }));
    view_menu.add_action(GUI::Action::create("3x size", { Mod_Ctrl, Key_3 }, [&](auto&) {
        player.zoom(3);
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("VideoPlayer", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-videoplayer.png"), window);
    }));

    app.set_menubar(menubar);

    if (argc > 1) {
        String path = argv[1];
        player.open_file(path);
    }

    window->show();
    return app.exec();
}