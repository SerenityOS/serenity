/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMapWidget.h"
#include "SearchCharacters.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

static void search_and_print_results(ByteString const& query)
{
    outln("Searching for '{}'", query);
    u32 result_count = 0;
    for_each_character_containing(query, [&](auto code_point, auto display_name) {
        StringBuilder builder;
        builder.append_code_point(code_point);
        builder.append(" - "sv);
        builder.append(display_name);
        outln(builder.string_view());
        result_count++;
        return IterationDecision::Continue;
    });

    if (result_count == 0)
        outln("No results found.");
    else if (result_count == 1)
        outln("1 result found.");
    else
        outln("{} results found.", result_count);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    auto app = TRY(GUI::Application::create(arguments));
    Config::pledge_domain("CharacterMap");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/CharacterMap.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    ByteString query;
    Core::ArgsParser args_parser;
    args_parser.add_option(query, "Search character names using this query, and print them as a list.", "search", 's', "query");
    args_parser.parse(arguments);

    if (!query.is_empty()) {
        search_and_print_results(query);
        return 0;
    }

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-character-map"sv));
    auto window = GUI::Window::construct();
    window->set_title("Character Map");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(600, 400);

    auto character_map_widget = window->set_main_widget<CharacterMapWidget>();
    TRY(character_map_widget->initialize_menubar(*window));

    auto font_query = Config::read_string("CharacterMap"sv, "History"sv, "Font"sv, Gfx::FontDatabase::the().default_font_query());
    character_map_widget->set_font(Gfx::FontDatabase::the().get_by_name(font_query));

    window->show();
    return app->exec();
}
