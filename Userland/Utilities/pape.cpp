/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibMain/Main.h>

static int handle_show_all()
{
    Core::DirIterator di("/res/wallpapers", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        warnln("DirIterator: {}", di.error_string());
        return 1;
    }

    while (di.has_next()) {
        String name = di.next_path();
        outln("{}", name);
    }
    return 0;
}

static int handle_show_current()
{
    outln("{}", GUI::Desktop::the().wallpaper());
    return 0;
}

static int handle_set_pape(const String& name)
{
    StringBuilder builder;
    builder.append("/res/wallpapers/");
    builder.append(name);
    String path = builder.to_string();
    if (!GUI::Desktop::the().set_wallpaper(path)) {
        warnln("pape: Failed to set wallpaper {}", path);
        return 1;
    }
    return 0;
};

static int handle_set_random()
{
    Vector<String> wallpapers;
    Core::DirIterator di("/res/wallpapers", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        warnln("DirIterator: {}", di.error_string());
        return 1;
    }
    while (di.has_next()) {
        wallpapers.append(di.next_full_path());
    }
    wallpapers.remove_all_matching([](const String& wallpaper) { return wallpaper == GUI::Desktop::the().wallpaper(); });
    if (wallpapers.is_empty()) {
        warnln("pape: No wallpapers found");
        return 1;
    }
    auto& wallpaper = wallpapers.at(get_random_uniform(wallpapers.size()));
    if (!GUI::Desktop::the().set_wallpaper(wallpaper)) {
        warnln("pape: Failed to set wallpaper {}", wallpaper);
        return 1;
    }
    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool show_all = false;
    bool show_current = false;
    bool set_random = false;
    const char* name = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(show_all, "Show all wallpapers", "show-all", 'a');
    args_parser.add_option(show_current, "Show current wallpaper", "show-current", 'c');
    args_parser.add_option(set_random, "Set random wallpaper", "set-random", 'r');
    args_parser.add_positional_argument(name, "Wallpaper to set", "name", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::try_create(arguments));

    if (show_all)
        return handle_show_all();
    else if (show_current)
        return handle_show_current();
    else if (set_random)
        return handle_set_random();

    return handle_set_pape(name);
}
