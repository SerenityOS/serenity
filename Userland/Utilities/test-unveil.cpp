/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    Vector<StringView> paths_to_test;
    const char* permissions = "r";
    bool should_sleep = false;

    Core::ArgsParser parser;
    parser.add_option(permissions, "Apply these permissions going forward", "permissions", 'p', "unveil-permissions");
    parser.add_option(should_sleep, "Sleep after processing all arguments", "sleep", 's');
    parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Add a path to the unveil list",
        .long_name = "unveil",
        .short_name = 'u',
        .value_name = "path",
        .accept_value = [&](auto* s) {
            StringView path { s };
            if (path.is_empty())
                return false;
            if (unveil(s, permissions) < 0) {
                perror("unveil");
                return false;
            }
            return true;
        } });
    parser.add_option(Core::ArgsParser::Option {
        .requires_argument = false,
        .help_string = "Lock the veil",
        .long_name = "lock",
        .short_name = 'l',
        .accept_value = [&](auto*) {
            if (unveil(nullptr, nullptr) < 0) {
                perror("unveil(nullptr, nullptr)");
                return false;
            }
            return true;
        } });
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Test a path against the veil",
        .name = "path",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](auto* s) {
            if (access(s, X_OK) == 0)
                warnln("'{}' - ok", s);
            else
                warnln("'{}' - fail: {}", s, strerror(errno));
            return true;
        } });

    parser.parse(argc, argv);
    if (should_sleep)
        sleep(INT_MAX);
    return 0;
}
