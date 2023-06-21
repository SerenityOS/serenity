/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <limits.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> paths_to_test;
    StringView permissions = "r"sv;
    bool should_sleep = false;

    Core::ArgsParser parser;
    parser.add_option(permissions, "Apply these permissions going forward", "permissions", 'p', "unveil-permissions");
    parser.add_option(should_sleep, "Sleep after processing all arguments", "sleep", 's');
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Add a path to the unveil list",
        .long_name = "unveil",
        .short_name = 'u',
        .value_name = "path",
        .accept_value = [&](StringView path) {
            if (path.is_empty())
                return false;
            auto maybe_error = Core::System::unveil(path, permissions);
            if (maybe_error.is_error()) {
                warnln("{}", maybe_error.error());
                return false;
            }
            return true;
        } });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::None,
        .help_string = "Lock the veil",
        .long_name = "lock",
        .short_name = 'l',
        .accept_value = [&](StringView) {
            auto maybe_error = Core::System::unveil(nullptr, nullptr);
            if (maybe_error.is_error()) {
                warnln("unveil(nullptr, nullptr): {}", maybe_error.error());
                return false;
            }
            return true;
        } });
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Test a path against the veil",
        .name = "path",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](StringView s) {
            auto maybe_error = Core::System::access(s, X_OK);
            if (maybe_error.is_error())
                warnln("'{}' - fail: {}", s, maybe_error.error());
            else
                warnln("'{}' - ok", s);
            return true;
        } });

    parser.parse(arguments);
    if (should_sleep)
        sleep(INT_MAX);
    return 0;
}
