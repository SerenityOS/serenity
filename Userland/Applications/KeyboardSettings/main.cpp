/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardSettingsWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

// Including this after to avoid LibIPC errors
#include <LibConfig/Client.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix proc exec"));
    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domain("KeyboardSettings");

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, only option is 'keyboard'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath recvfd sendfd proc exec"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/keymap", "x"));
    TRY(Core::System::unveil("/proc/keymap", "r"));
    TRY(Core::System::unveil("/etc/Keyboard.ini", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-keyboard-settings");

    auto window = TRY(GUI::SettingsWindow::create("Keyboard Settings"));
    window->set_icon(app_icon.bitmap_for_size(16));
    auto keyboard_settings_widget = TRY(window->add_tab<KeyboardSettingsWidget>("Keyboard", "keyboard"));
    window->set_active_tab(selected_tab);

    window->on_active_window_change = [&](bool is_active_window) {
        keyboard_settings_widget->window_activated(is_active_window);
    };

    window->show();
    return app->exec();
}
