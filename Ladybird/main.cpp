/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "WebView.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibMain/Main.h>
#include <QApplication>
#include <QWidget>

extern void initialize_web_engine();

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    initialize_web_engine();

    String url;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("The Ladybird web browser :^)");
    args_parser.add_positional_argument(url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Core::EventLoop event_loop;

    QApplication app(arguments.argc, arguments.argv);
    BrowserWindow window(event_loop);
    window.setWindowTitle("Ladybird");
    window.resize(800, 600);
    window.show();

    auto qt_event_loop_driver = Core::Timer::create_repeating(50, [&] {
        app.processEvents();
    });
    qt_event_loop_driver->start();

    if (!url.is_empty()) {
        window.view().load(url);
    }

    return event_loop.exec();
}
