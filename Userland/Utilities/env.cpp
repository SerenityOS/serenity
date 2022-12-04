/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath exec"));

    bool ignore_env = false;
    StringView split_string {};
    Vector<DeprecatedString> values;

    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);

    args_parser.add_option(ignore_env, "Start with an empty environment", "ignore-environment", 'i');
    args_parser.add_option(split_string, "Process and split S into separate arguments; used to pass multiple arguments on shebang lines", "split-string", 'S', "S");

    args_parser.add_positional_argument(values, "Environment and commands", "env/command", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (ignore_env)
        clearenv();

    size_t argv_start;
    for (argv_start = 0; argv_start < values.size(); ++argv_start) {
        if (values[argv_start].contains('=')) {
            putenv(const_cast<char*>(values[argv_start].characters()));
        } else {
            break;
        }
    }

    Vector<StringView> new_argv;
    if (!split_string.is_empty()) {
        for (auto view : split_string.split_view(' ')) {
            new_argv.append(view);
        }
    }

    for (size_t i = argv_start; i < values.size(); ++i) {
        new_argv.append(values[i]);
    }

    if (new_argv.size() == 0) {
        for (auto entry = environ; *entry != nullptr; ++entry)
            outln("{}", *entry);

        return 0;
    }

    TRY(Core::System::exec(new_argv[0], new_argv, Core::System::SearchInPath::Yes));
    return 1;
}
