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

    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = GUI::Icon::default_icon("app-keyboard-mapper"sv);

    auto window = GUI::Window::construct();
    window->set_title("Keyboard Mapper");
    window->set_icon(app_icon.bitmap_for_size(16));
    auto keyboard_mapper_widget = window->set_main_widget<KeyboardMapperWidget>();
    window->restore_size_and_position("KeyboardMapper"sv, "Window"sv, { { 775, 315 } });
    window->save_size_and_position_on_close("KeyboardMapper"sv, "Window"sv);
    window->set_resizable(false);

    if (path.is_empty())
        TRY(keyboard_mapper_widget->load_map_from_system());
    else
        TRY(keyboard_mapper_widget->load_map_from_file(path));

    TRY(Core::System::pledge("stdio thread rpath cpath wpath recvfd sendfd unix"));

    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            if (!keyboard_mapper_widget->request_close())
                return;

            Optional<ByteString> path = GUI::FilePicker::get_open_filepath(window, "Open"sv, "/res/keymaps/"sv);
            if (!path.has_value())
                return;

            if (auto error_or = keyboard_mapper_widget->load_map_from_file(path.value()); error_or.is_error())
                keyboard_mapper_widget->show_error_to_user(error_or.release_error());
        });

    auto save_action = GUI::CommonActions::make_save_action(
        [&](auto&) {
            if (auto error_or = keyboard_mapper_widget->save(); error_or.is_error())
                keyboard_mapper_widget->show_error_to_user(error_or.release_error());
        });

    auto save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        ByteString name = "Unnamed";
        Optional<ByteString> save_path = GUI::FilePicker::get_save_filepath(window, name, "json");
        if (!save_path.has_value())
            return;

        if (auto error_or = keyboard_mapper_widget->save_to_file(save_path.value()); error_or.is_error())
            keyboard_mapper_widget->show_error_to_user(error_or.release_error());
    });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        },
        GUI::CommonActions::QuitAltShortcut::None);

    auto auto_modifier_action = GUI::Action::create("Auto-Modifier", [&](auto& act) {
        keyboard_mapper_widget->set_automatic_modifier(act.is_checked());
    });
    auto_modifier_action->set_status_tip("Toggle automatic modifier"_string);
    auto_modifier_action->set_checkable(true);
    auto_modifier_action->set_checked(false);

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(open_action);
    file_menu->add_action(save_action);
    file_menu->add_action(save_as_action);
    file_menu->add_separator();
    file_menu->add_action(quit_action);

    auto settings_menu = window->add_menu("&Settings"_string);
    settings_menu->add_action(auto_modifier_action);

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Keyboard Mapper"_string, app_icon, window));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (keyboard_mapper_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    return app->exec();
}
