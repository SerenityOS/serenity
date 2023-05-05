/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowServer.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    int flash_flush = -1;
    Core::ArgsParser args_parser;
    args_parser.add_option(flash_flush, "Flash flush (repaint) rectangles", "flash-flush", 'f', "0/1");
    args_parser.parse(arguments);

    if (flash_flush != -1)
        GUI::ConnectionToWindowServer::the().async_set_flash_flush(flash_flush);
    return 0;
}
