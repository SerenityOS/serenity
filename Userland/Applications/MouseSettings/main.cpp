/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseWidget.h"
#include "ThemeWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio cpath rpath recvfd sendfd"));

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, one of 'cursor-theme' or 'mouse'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    auto app_icon = GUI::Icon::default_icon("app-mouse");

    auto window = TRY(GUI::SettingsWindow::create("Mouse Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    (void)TRY(window->add_tab<MouseWidget>("Mouse", "mouse"));
    (void)TRY(window->add_tab<ThemeWidget>("Cursor Theme", "cursor-theme"));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_active_tab(selected_tab);

    window->show();
    return app->exec();
}
