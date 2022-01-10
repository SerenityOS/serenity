/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMapWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibMain/Main.h>
#include <LibUnicode/CharacterTypes.h>

static void search_and_print_results(String const& query)
{
    outln("Searching for '{}'", query);
    String uppercase_query = query.to_uppercase();
    StringView uppercase_query_view = uppercase_query.view();
    // FIXME: At time of writing there are 144,697 code points in Unicode. I've added some breathing room,
    //        but ideally this would be defined in LibUnicode somewhere.
    constexpr u32 unicode_character_count = 150000;
    // FIXME: There's probably a better way to do this than just looping, but it still only takes ~150ms to run for me!
    u32 result_count = 0;
    for (u32 i = 1; i < unicode_character_count; ++i) {
        if (auto maybe_display_name = Unicode::code_point_display_name(i); maybe_display_name.has_value()) {
            auto& display_name = maybe_display_name.value();
            // FIXME: This should be a case-sensitive search, since we already converted the query to uppercase
            //        and the unicode names are all in uppercase. But, that makes it run slower!
            //        Sensitive: ~175ms, Insensitive: ~140ms
            if (display_name.contains(uppercase_query_view, AK::CaseSensitivity::CaseInsensitive)) {
                StringBuilder builder;
                builder.append_code_point(i);
                builder.append(" - ");
                builder.append(display_name);
                outln(builder.string_view());
                result_count++;
            }
        }
    }

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

    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domains("CharacterMap");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    String query;
    Core::ArgsParser args_parser;
    args_parser.add_option(query, "Search character names using this query, and print them as a list.", "search", 's', "query");
    args_parser.parse(arguments);

    if (!query.is_empty()) {
        search_and_print_results(query);
        return 0;
    }

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-keyboard-settings"));
    auto window = TRY(GUI::Window::try_create());
    window->set_title("Character Map");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(400, 400);

    auto character_map_widget = TRY(window->try_set_main_widget<CharacterMapWidget>());
    character_map_widget->initialize_menubar(*window);

    auto font_query = Config::read_string("CharacterMap", "History", "Font", Gfx::FontDatabase::the().default_font_query());
    character_map_widget->set_font(Gfx::FontDatabase::the().get_by_name(font_query));

    window->show();
    return app->exec();
}
