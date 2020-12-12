/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Hex.h>
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <LibBitTorrent/HTTPTracker.h>
#include <LibBitTorrent/Tracker.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>

int main(int argc, char** argv)
{
    const char* url_string = nullptr;
    Vector<const char*> info_hash_strings;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Scrape a BitTorrent tracker to get statistics.");
    args_parser.add_positional_argument(url_string, "URL of the tracker", "url");
    args_parser.add_positional_argument(info_hash_strings, "info_hash to query the tracker for (as hex, potentially multiple)", "info_hash");
    args_parser.parse(argc, argv);

    URL url(url_string);
    if (!url.is_valid()) {
        out("'{}' is not a valid URL\n", url_string);
        return 1;
    }

    ScrapeRequest request;

    for (auto& info_hash_string : info_hash_strings) {
        if (strlen(info_hash_string) != 40) {
            out("'{}' is not a valid info_hash; it should be 40 characters\n", info_hash_string);
            return 1;
        }

        auto info_hash = decode_hex(info_hash_string);
        if (!info_hash.has_value()) {
            out("'{}' is not a valid info_hash; parsing it as hex failed\n", info_hash_string);
            return 1;
        }

        request.add_info_hash(move(info_hash.value()));
    }

    Core::EventLoop loop;

    request.on_error = [&loop]() {
        out("Scrape failed!\n");
        loop.quit(1);
    };
    request.on_success = [&loop](ScrapeResponse&& response) {
        for (auto& file : response.files())
            out(
                "{}: complete={} downloaded={} incomplete={}\n",
                encode_hex(file.info_hash()),
                file.complete(), file.downloaded(), file.incomplete());
        loop.quit(0);
    };

    HTTPTracker tracker(url);

    tracker.scrape(request);

    return loop.exec();
}
