/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Rasmus Nylander <RasmusNylander.SerenityOS@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardMapperWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView path;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Keyboard character mapping file.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio getkeymap thread rpath cpath wpath recvfd sendfd unix"));

    auto app = GUI::Application::construct(arguments.argc, arguments.argv);

    TRY(Core::System::pledge("stdio getkeymap thread rpath cpath wpath recvfd sendfd"));

    auto app_icon = GUI::Icon::default_icon("app-keyboard-mapper");

    auto window = GUI::Window::construct();
    window->set_title("Keyboard Mapper");
    window->set_icon(app_icon.bitmap_for_size(16));
    auto keyboard_mapper_widget = TRY(window->try_set_main_widget<KeyboardMapperWidget>());
    window->resize(775, 315);
    window->set_resizable(false);

    if (path.is_empty())
        TRY(keyboard_mapper_widget->load_map_from_system());
    else
        TRY(keyboard_mapper_widget->load_map_from_file(path));

    TRY(Core::System::pledge("stdio thread rpath cpath wpath recvfd sendfd"));

    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open", "/res/keymaps/");
            if (!path.has_value())
                return;

            ErrorOr<void> error_or = keyboard_mapper_widget->load_map_from_file(path.value());
            if (error_or.is_error())
                keyboard_mapper_widget->show_error_to_user(error_or.error());
        });

    auto save_action = GUI::CommonActions::make_save_action(
        [&](auto&) {
            ErrorOr<void> error_or = keyboard_mapper_widget->save();
            if (error_or.is_error())
                keyboard_mapper_widget->show_error_to_user(error_or.error());
        });

    auto save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        String name = "Unnamed";
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, name, "json");
        if (!save_path.has_value())
            return;

        ErrorOr<void> error_or = keyboard_mapper_widget->save_to_file(save_path.value());
        if (error_or.is_error())
            keyboard_mapper_widget->show_error_to_user(error_or.error());
    });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto auto_modifier_action = GUI::Action::create("Auto Modifier", [&](auto& act) {
        keyboard_mapper_widget->set_automatic_modifier(act.is_checked());
    });
    auto_modifier_action->set_status_tip("Toggle automatic modifier");
    auto_modifier_action->set_checkable(true);
    auto_modifier_action->set_checked(false);

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(open_action);
    file_menu.add_action(save_action);
    file_menu.add_action(save_as_action);
    file_menu.add_separator();
    file_menu.add_action(quit_action);

    auto& settings_menu = window->add_menu("&Settings");
    settings_menu.add_action(auto_modifier_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Keyboard Mapper", app_icon, window));

    window->show();

    return app->exec();
}
