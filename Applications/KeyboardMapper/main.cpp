/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include "KeyboardMapperWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>

int main(int argc, char** argv)
{
    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Keyboard character mapping file.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-keyboard-mapper");

    auto window = GUI::Window::construct();
    window->set_title("KeyboardMapper");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_main_widget<KeyboardMapperWidget>();
    window->resize(775, 315);
    window->set_resizable(false);
    window->show();

    auto keyboard_mapper_widget = (KeyboardMapperWidget*)window->main_widget();
    if (path != nullptr) {
        keyboard_mapper_widget->load_from_file(path);
    } else {
        keyboard_mapper_widget->load_from_file("/res/keymaps/en.json");
    }

    // Actions
    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open");
            if (path.has_value()) {
                keyboard_mapper_widget->load_from_file(path.value());
            }
        });

    auto save_action = GUI::CommonActions::make_save_action(
        [&](auto&) {
            keyboard_mapper_widget->save();
        });

    auto save_as_action = GUI::Action::create("Save as...", { Mod_Ctrl | Mod_Shift, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"),
        [&](auto&) {
            String m_name = "Unnamed";
            Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, m_name, "json");
            if (!save_path.has_value())
                return;

            keyboard_mapper_widget->save_to_file(save_path.value());
        });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto about_action = GUI::Action::create("About",
        [&](auto&) {
            GUI::AboutDialog::show("KeyboardMapper", app_icon.bitmap_for_size(32), window);
        });

    // Menu
    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("KeyboardMapper");
    app_menu.add_action(open_action);
    app_menu.add_action(save_action);
    app_menu.add_action(save_as_action);
    app_menu.add_separator();
    app_menu.add_action(quit_action);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(about_action);

    app->set_menubar(move(menubar));

    return app->exec();
}
