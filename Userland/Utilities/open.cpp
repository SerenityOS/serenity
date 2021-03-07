/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
