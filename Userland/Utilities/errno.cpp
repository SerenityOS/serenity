/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <string.h>

Array<StringView, EMAXERRNO + 1> s_errno_names = {
#define __ENUMERATE_ERRNO_CODE(c, s) #c##sv,
    ENUMERATE_ERRNO_CODES(__ENUMERATE_ERRNO_CODE)
#undef __ENUMERATE_ERRNO_CODE
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool list = false;
    bool search = false;
    StringView keyword;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(keyword, "Error number or name to look up", "keyword", Core::ArgsParser::Required::No);
    args_parser.add_option(list, "List all errno values", "list", 'l');
    args_parser.add_option(search, "Search for error descriptions containing keyword", "search", 's');
    args_parser.parse(arguments);

    auto max_name_length = 0;
    for (int i = 0; i < sys_nerr; i++) {
        max_name_length = max(max_name_length, s_errno_names[i].length());
    }

    auto output_errno_description = [max_name_length](int errno_code, auto error_string) {
        outln("{:{}} {:>2} {}", s_errno_names[errno_code], max_name_length, errno_code, error_string);
    };

    if (list) {
        for (int i = 0; i < sys_nerr; i++)
            output_errno_description(i, strerror(i));
        return 0;
    }

    if (keyword.is_empty())
        return 0;

    if (search) {
        for (int i = 0; i < sys_nerr; i++) {
            auto error_string = strerror(i);
            StringView error { error_string, strlen(error_string) };
            if (error.contains(keyword, CaseSensitivity::CaseInsensitive))
                output_errno_description(i, error);
        }
        return 0;
    }

    if (auto maybe_error_code = keyword.to_number<int>(); maybe_error_code.has_value()) {
        auto error_code = maybe_error_code.value();
        auto error = strerror(error_code);
        if (error == "Unknown error"sv) {
            warnln("ERROR: Unknown errno: {}", keyword);
            return 1;
        }
        output_errno_description(error_code, error);
        return 0;
    }

    for (int i = 0; i < sys_nerr; i++) {
        if (keyword.equals_ignoring_ascii_case(s_errno_names[i])) {
            output_errno_description(i, strerror(i));
            return 0;
        }
    }

    warnln("ERROR: Not understood: {}", keyword);
    return 1;
}
