/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGfx/Rect.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio unix recvfd sendfd"));

    Core::EventLoop event_loop;
    int window_id;
    Optional<size_t> x, y, width, height;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(window_id, "Window to manipulate", "id", Core::ArgsParser::Required::Yes);
    args_parser.add_option(x, "X coordinate to move the window to", "absolute-x", 'x', "x coordinate");
    args_parser.add_option(y, "Y coordinate to move the window to", "absolute-y", 'y', "y coordinate");
    args_parser.add_option(width, "Width of the window", "width", 'w', "width");
    args_parser.add_option(height, "Height of the window", "height", 'h', "height");
    args_parser.parse(arguments);

    if (!GUI::ConnectionToWindowManagerServer::the().window_exists(window_id)) {
        warnln("Window does not exist");
        return 1;
    }

    auto rect = GUI::ConnectionToWindowManagerServer::the().get_window_rect(window_id);
    if (x.has_value())
        rect.set_x(x.value());
    if (y.has_value())
        rect.set_y(y.value());
    if (width.has_value())
        rect.set_width(width.value());
    if (height.has_value())
        rect.set_height(height.value());
    GUI::ConnectionToWindowManagerServer::the().async_set_window_rect(window_id, rect);

    return 0;
}
