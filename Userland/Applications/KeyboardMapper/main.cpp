/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardMapperWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Keyboard character mapping file.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (pledge("stdio getkeymap thread rpath cpath wpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio getkeymap thread rpath cpath wpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-keyboard-mapper");

    auto window = GUI::Window::construct();
    window->set_title("Keyboard Mapper");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_main_widget<KeyboardMapperWidget>();
    window->resize(775, 315);
    window->set_resizable(false);

    auto keyboard_mapper_widget = (KeyboardMapperWidget*)window->main_widget();
    if (path != nullptr) {
        keyboard_mapper_widget->load_from_file(path);
    } else {
        keyboard_mapper_widget->load_from_system();
    }

    if (pledge("stdio thread rpath cpath wpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open", "/res/keymaps/");
            if (path.has_value()) {
                keyboard_mapper_widget->load_from_file(path.value());
            }
        });

    auto save_action = GUI::CommonActions::make_save_action(
        [&](auto&) {
            keyboard_mapper_widget->save();
        });

    auto save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        String name = "Unnamed";
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, name, "json");
        if (!save_path.has_value())
            return;

        keyboard_mapper_widget->save_to_file(save_path.value());
    });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(open_action);
    file_menu.add_action(save_action);
    file_menu.add_action(save_as_action);
    file_menu.add_separator();
    file_menu.add_action(quit_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Keyboard Mapper", app_icon, window));

    window->show();

    return app->exec();
}
