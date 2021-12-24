/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath exec"));

    bool ignore_env = false;
    const char* split_string = nullptr;
    Vector<const char*> values;

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
        if (StringView { values[argv_start] }.contains('=')) {
            putenv(const_cast<char*>(values[argv_start]));
        } else {
            break;
        }
    }

    Vector<String> split_string_storage;
    Vector<const char*> new_argv;
    if (split_string) {
        for (auto view : StringView(split_string).split_view(' ')) {
            split_string_storage.append(view);
        }
        for (auto& str : split_string_storage) {
            new_argv.append(str.characters());
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

    new_argv.append(nullptr);

    const char* executable = new_argv[0];
    char* const* new_argv_ptr = const_cast<char* const*>(&new_argv[0]);

    execvp(executable, new_argv_ptr);
    perror("execvp");
    return 1;
}
