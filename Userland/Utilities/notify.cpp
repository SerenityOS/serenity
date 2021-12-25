/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Notification.h>
#include <LibGfx/Bitmap.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    Core::ArgsParser args_parser;
    const char* title = nullptr;
    const char* message = nullptr;
    const char* icon_path = nullptr;
    args_parser.add_positional_argument(title, "Title of the notification", "title");
    args_parser.add_positional_argument(message, "Message to display in the notification", "message");
    args_parser.add_positional_argument(icon_path, "Path of icon to display in the notification", "icon-path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto notification = GUI::Notification::construct();
    notification->set_text(message);
    notification->set_title(title);
    if (icon_path) {
        ErrorOr<NonnullRefPtr<Gfx::Bitmap>> icon_or_error = Gfx::Bitmap::try_load_from_file(icon_path);
        if (icon_or_error.is_error()) {
            warnln("Failed to load icon: {}", icon_or_error.error());
            return 1;
        }
        notification->set_icon(icon_or_error.release_value());
    }
    notification->show();

    return 0;
}
