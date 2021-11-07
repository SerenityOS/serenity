/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <string.h>

int main(int argc, char** argv)
{
    bool list = false;
    bool search = false;
    const char* keyword = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(keyword, "Error number or string to search", "keyword", Core::ArgsParser::Required::No);
    args_parser.add_option(list, "List all errno values", "list", 'l');
    args_parser.add_option(search, "Search for error descriptions containing keyword", "search", 's');
    args_parser.parse(argc, argv);

    if (list) {
        for (int i = 0; i < sys_nerr; i++) {
            outln("{} {}", i, strerror(i));
        }
        return 0;
    }

    if (keyword == nullptr)
        return 0;

    if (search) {
        for (int i = 0; i < sys_nerr; i++) {
            auto error = String::formatted("{}", strerror(i));
            if (error.contains(keyword, CaseSensitivity::CaseInsensitive)) {
                outln("{} {}", i, error);
            }
        }
        return 0;
    }

    auto maybe_errno = StringView { keyword }.to_int();
    if (!maybe_errno.has_value()) {
        warnln("ERROR: Not understood: {}", keyword);
        return 1;
    }

    auto error = String::formatted("{}", strerror(maybe_errno.value()));
    if (error == "Unknown error") {
        warnln("ERROR: Unknown errno: {}", keyword);
        return 1;
    }
    outln("{} {}", keyword, error);

    return 0;
}
