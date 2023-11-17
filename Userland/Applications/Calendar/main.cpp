/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalendarWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Process.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath proc exec unix"));

    auto app = TRY(GUI::Application::create(arguments));

    StringView filename;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "File to read from", "file", Core::ArgsParser::Required::No);

    args_parser.parse(arguments);

    if (!filename.is_empty()) {
        if (!FileSystem::exists(filename) || FileSystem::is_directory(filename)) {
            warnln("File does not exist or is a directory: {}", filename);
            return 1;
        }
    }

    Config::pledge_domain("Calendar");
    Config::monitor_domain("Calendar");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath proc exec unix"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/CalendarSettings", "x"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-calendar"sv));
    auto window = GUI::Window::construct();
    window->set_title("Calendar");
    window->restore_size_and_position("Calendar"sv, "Window"sv, { { 600, 480 } });
    window->save_size_and_position_on_close("Calendar"sv, "Window"sv);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto calendar_widget = TRY(Calendar::CalendarWidget::create(window));
    window->set_main_widget(calendar_widget);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (calendar_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    if (!filename.is_empty()) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, filename);
        if (!response.is_error()) {
            calendar_widget->load_file(response.release_value());
        }
    }

    app->exec();

    return 0;
}
