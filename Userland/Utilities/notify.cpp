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
    auto app = TRY(GUI::Application::try_create(arguments));

    Core::ArgsParser args_parser;
    StringView title {};
    StringView message {};
    StringView icon_path {};
    TRY(args_parser.add_positional_argument(title, "Title of the notification", "title"));
    TRY(args_parser.add_positional_argument(message, "Message to display in the notification", "message"));
    TRY(args_parser.add_positional_argument(icon_path, "Path of icon to display in the notification", "icon-path", Core::ArgsParser::Required::No));
    TRY(args_parser.parse(arguments));

    auto notification = TRY(GUI::Notification::try_create());
    notification->set_text(message);
    notification->set_title(title);
    if (!icon_path.is_empty()) {
        notification->set_icon(TRY(Gfx::Bitmap::load_from_file(icon_path)));
    }
    notification->show();

    return 0;
}
