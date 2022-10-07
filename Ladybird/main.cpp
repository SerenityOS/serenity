/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "Settings.h"
#include "Utilities.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>
#include <QApplication>

Browser::Settings* s_settings;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    platform_init();

    // NOTE: We only instantiate this to ensure that Gfx::FontDatabase has its default queries initialized.
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    // NOTE: This is only used for the Core::Socket inside the IPC connections.
    // FIXME: Refactor things so we can get rid of this somehow.
    Core::EventLoop event_loop;

    QApplication app(arguments.argc, arguments.argv);

    String url;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("The Ladybird web browser :^)");
    args_parser.add_positional_argument(url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    BrowserWindow window;
    s_settings = new Browser::Settings(&window);
    window.setWindowTitle("Ladybird");
    window.resize(800, 600);
    window.show();

    if (!url.is_empty()) {
        window.view().load(url);
    }

    return app.exec();
}
