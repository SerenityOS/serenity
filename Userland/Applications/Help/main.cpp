/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

using namespace Help;

static String parse_input(StringView input)
{
    AK::URL url(input);
    if (url.is_valid())
        return url.basename();

    return input;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix proc"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::unveil("/proc/all", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/usr/share/man", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    String start_page;
    u32 section = 0;

    Core::ArgsParser args_parser;
    // FIXME: These custom Args are a hack. What we want to do is have an optional int arg, then an optional string.
    // However, when only a string is provided, it gets forwarded to the int argument since that is first, and
    // parsing fails. This hack instead forwards it to the start_page in that case.
    args_parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Section of the man page",
        .name = "section",
        .min_values = 0,
        .max_values = 1,
        .accept_value = [&](char const* input_ptr) {
            StringView input { input_ptr, strlen(input_ptr) };
            // If it's a number, use it as the section
            if (auto number = input.to_int(); number.has_value()) {
                section = number.value();
                return true;
            }

            // Otherwise, use it as the start_page
            start_page = parse_input(input);
            return true;
        } });
    args_parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Help page to open. Either an absolute path to the markdown file, or a search query",
        .name = "page",
        .min_values = 0,
        .max_values = 1,
        .accept_value = [&](char const* input_ptr) {
            StringView input { input_ptr, strlen(input_ptr) };
            // If start_page was already set by our section arg, then it can't be set again
            if (start_page.is_empty())
                return false;
            start_page = parse_input(input);
            return true;
        } });
    args_parser.parse(arguments);

    auto app_icon = GUI::Icon::default_icon("app-help"sv);

    auto window = TRY(GUI::Window::try_create());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Help");
    window->resize(570, 500);

    auto main_widget = TRY(window->try_set_main_widget<MainWidget>());
    TRY(main_widget->initialize_fallibles(window));
    main_widget->set_start_page(start_page, section);

    window->show();

    return app->exec();
}
