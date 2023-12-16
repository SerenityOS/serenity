/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, James Puleo <james@jame.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath unix sendfd recvfd"));

    bool show_all = false;
    bool show_current = false;
    bool set_random = false;
    StringView path;

    Core::ArgsParser args_parser;
    args_parser.add_option(show_all, "Show all wallpapers", "show-all", 'a');
    args_parser.add_option(show_current, "Show current wallpaper", "show-current", 'c');
    args_parser.add_option(set_random, "Set random wallpaper", "set-random", 'r');
    args_parser.add_positional_argument(path, "Wallpaper to set", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio rpath unix sendfd"));

    if (show_all) {
        Core::DirIterator wallpapers_directory_iterator("/res/wallpapers", Core::DirIterator::SkipDots);
        if (wallpapers_directory_iterator.has_error())
            return Error::from_string_literal("Unable to iterate /res/wallpapers directory");

        while (wallpapers_directory_iterator.has_next()) {
            auto name = wallpapers_directory_iterator.next_path();
            outln("{}", name);
        }
    } else if (show_current) {
        auto current_wallpaper_path = GUI::Desktop::the().wallpaper_path();
        outln("{}", current_wallpaper_path);
    } else if (set_random) {
        Core::DirIterator wallpapers_directory_iterator("/res/wallpapers", Core::DirIterator::SkipDots);
        if (wallpapers_directory_iterator.has_error())
            return Error::from_string_literal("Unable to iterate /res/wallpapers directory");

        Vector<ByteString> wallpaper_paths;

        auto current_wallpaper_path = GUI::Desktop::the().wallpaper_path();
        while (wallpapers_directory_iterator.has_next()) {
            auto next_full_path = wallpapers_directory_iterator.next_full_path();
            if (next_full_path != current_wallpaper_path)
                wallpaper_paths.append(move(next_full_path));
        }

        if (wallpaper_paths.is_empty())
            return Error::from_string_literal("No wallpapers found");

        auto& chosen_wallpaper_path = wallpaper_paths.at(get_random_uniform(wallpaper_paths.size()));
        auto chosen_wallpaper_bitmap = TRY(Gfx::Bitmap::load_from_file(chosen_wallpaper_path));
        if (!GUI::Desktop::the().set_wallpaper(chosen_wallpaper_bitmap, chosen_wallpaper_path))
            return Error::from_string_literal("Failed to set wallpaper");

        outln("Set wallpaper to {}", chosen_wallpaper_path);
    } else {
        if (path.is_null())
            return Error::from_string_literal("Must provide a path to a wallpaper");

        auto wallpaper_bitmap = TRY(Gfx::Bitmap::load_from_file(path));
        if (!GUI::Desktop::the().set_wallpaper(wallpaper_bitmap, path))
            return Error::from_string_literal("Failed to set wallpaper");
    }
    return 0;
}
