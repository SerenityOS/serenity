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
#include <string.h>

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
        auto url = URL::create_with_url_or_path(url_or_path);

        if (url.protocol() == "file") {
            // NOTE: Since URL::create_with_url_or_path() returns "file:///" for ".", and we chose
            // to fix that in open(1) itself using Core::File::real_path_for(), we have to
            // conditionally chose either the URL's path or user-specified argument (also a path).
            auto real_path = Core::File::real_path_for(StringView(url_or_path).starts_with("file://") ? url.path() : url_or_path);
            if (real_path.is_null()) {
                // errno *should* be preserved from Core::File::real_path_for().
                warnln("Failed to open '{}': {}", url.path(), strerror(errno));
                all_ok = false;
                continue;
            }
            url = URL::create_with_url_or_path(real_path);
        }

        if (!Desktop::Launcher::open(url)) {
            warnln("Failed to open '{}'", url);
            all_ok = false;
        }
    }

    return all_ok ? 0 : 1;
}
