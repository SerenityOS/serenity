/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>

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

int main(int argc, char** argv)
{
    bool show_all = false;
    bool show_current = false;
    const char* name = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(show_all, "Show all wallpapers", "show-all", 'a');
    args_parser.add_option(show_current, "Show current wallpaper", "show-current", 'c');
    args_parser.add_positional_argument(name, "Wallpaper to set", "name", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    if (show_all)
        return handle_show_all();
    else if (show_current)
        return handle_show_current();

    return handle_set_pape(name);
}
