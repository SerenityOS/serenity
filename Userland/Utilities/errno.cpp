/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool list = false;
    bool search = false;
    StringView keyword;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(keyword, "Error number or string to search", "keyword", Core::ArgsParser::Required::No);
    args_parser.add_option(list, "List all errno values", "list", 'l');
    args_parser.add_option(search, "Search for error descriptions containing keyword", "search", 's');
    args_parser.parse(arguments);

    if (list) {
        for (int i = 0; i < sys_nerr; i++) {
            outln("{} {}", i, strerror(i));
        }
        return 0;
    }

    if (keyword.is_empty())
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

    auto maybe_errno = keyword.to_int();
    if (!maybe_errno.has_value()) {
        warnln("ERROR: Not understood: {}", keyword);
        return 1;
    }

    auto error = String::formatted("{}", strerror(maybe_errno.value()));
    if (error == "Unknown error"sv) {
        warnln("ERROR: Unknown errno: {}", keyword);
        return 1;
    }
    outln("{} {}", keyword, error);

    return 0;
}
