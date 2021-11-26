/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>

int main(int argc, char* argv[])
{
    Core::EventLoop loop;
    Vector<const char*> urls_or_paths;
    Core::ArgsParser parser;
    parser.set_general_help("Open a file or URL by executing the appropriate program.");
    parser.add_positional_argument(urls_or_paths, "URL or file path to open", "url-or-path");
    parser.parse(argc, argv);

    bool all_ok = true;

    for (auto& url_or_path : urls_or_paths) {
        auto path = Core::File::real_path_for(url_or_path);
        auto url = URL::create_with_url_or_path(path.is_null() ? url_or_path : path);

        if (!Desktop::Launcher::open(url)) {
            warnln("Failed to open '{}'", url);
            all_ok = false;
        }
    }

    return all_ok ? 0 : 1;
}
