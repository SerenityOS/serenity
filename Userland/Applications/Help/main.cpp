/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

using namespace Help;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::unveil("/res", "r"));
    // We specifically don't want to load this path from a library, as that can be hijacked with LD_PRELOAD.
    TRY(Core::System::unveil("/usr/share/man", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    ByteString first_query_parameter;
    ByteString second_query_parameter;

    Core::ArgsParser args_parser;
    // The actual "page query" parsing happens when we set the main widget's start page.
    args_parser.add_positional_argument(
        first_query_parameter,
        "Section of the man page",
        "section",
        Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(
        second_query_parameter,
        "Help page to open. Either an absolute path to the markdown file, or a search query",
        "page",
        Core::ArgsParser::Required::No);
    args_parser.parse(arguments);
    Vector<StringView, 2> query_parameters;
    if (!first_query_parameter.is_empty())
        query_parameters.append(first_query_parameter);
    if (!second_query_parameter.is_empty())
        query_parameters.append(second_query_parameter);

    auto app_icon = GUI::Icon::default_icon("app-help"sv);

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Help");
    window->resize(570, 500);

    auto main_widget = TRY(MainWidget::try_create());
    window->set_main_widget(main_widget);

    TRY(main_widget->initialize(window));
    TRY(main_widget->set_start_page(query_parameters));

    window->show();

    return app->exec();
}
