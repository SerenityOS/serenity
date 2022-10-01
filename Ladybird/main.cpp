/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "Settings.h"
#include "SimpleWebView.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/Timer.h>
#include <LibMain/Main.h>
#include <QApplication>
#include <QWidget>

extern void initialize_web_engine();
Browser::Settings* s_settings;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    QApplication app(arguments.argc, arguments.argv);

    initialize_web_engine();

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
