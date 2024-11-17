/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Notification.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    Core::ArgsParser args_parser;
    String title {};
    String message {};
    StringView icon_path {};
    StringView launch_url {};
    args_parser.add_positional_argument(title, "Title of the notification", "title");
    args_parser.add_positional_argument(message, "Message to display in the notification", "message");
    args_parser.add_option(icon_path, "Path of icon to display in the notification", "icon-path", 'I', "icon_path");
    args_parser.add_option(launch_url, "Launch URL for the notification", "launch-url", 'L', "launch_url");
    args_parser.parse(arguments);

    auto notification = GUI::Notification::construct();
    notification->set_text(message);
    notification->set_title(title);
    if (!icon_path.is_empty()) {
        notification->set_icon(TRY(Gfx::Bitmap::load_from_file(icon_path)));
    }
    notification->set_launch_url(launch_url);
    notification->show();

    return 0;
}
