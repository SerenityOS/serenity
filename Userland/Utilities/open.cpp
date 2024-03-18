/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop loop;
    Vector<StringView> urls_or_paths;
    Core::ArgsParser parser;
    parser.set_general_help("Open a file or URL by executing the appropriate program.");
    parser.add_positional_argument(urls_or_paths, "URL or file path to open", "url-or-path");
    parser.parse(arguments);

    bool all_ok = true;

    for (auto& url_or_path : urls_or_paths) {
        auto path_or_error = FileSystem::real_path(url_or_path);
        URL::URL url;
        if (path_or_error.is_error()) {
            url = url_or_path;
            if (!url.is_valid()) {
                warnln("Failed to open: '{}': {}", url_or_path, strerror(path_or_error.error().code()));
                continue;
            }
        } else {
            url = URL::create_with_url_or_path(path_or_error.value());
        }

        if (!Desktop::Launcher::open(url)) {
            warnln("Failed to open '{}'", url);
            all_ok = false;
        }
    }

    return all_ok ? 0 : 1;
}
