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
    Vector<ByteString> values_to_set;
    Vector<ByteString> values_to_unset;

    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);

    args_parser.add_option(ignore_env, "Start with an empty environment", "ignore-environment", 'i');
    args_parser.add_option(split_string, "Process and split S into separate arguments; used to pass multiple arguments on shebang lines", "split-string", 'S', "S");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Remove variable from the environment",
        .long_name = "unset",
        .short_name = 'u',
        .value_name = "name",
        .accept_value = [&values_to_unset](StringView value) {
            values_to_unset.append(value);
            return true;
        },
    });

    args_parser.add_positional_argument(values_to_set, "Environment and commands", "env/command", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (ignore_env) {
        clearenv();
    } else {
        for (auto const& value : values_to_unset) {
            if (unsetenv(value.characters()) < 0) {
                warnln("env: cannot unset '{}': {}", value, strerror(errno));
                return 1;
            }
        }
    }

    size_t argv_start;
    for (argv_start = 0; argv_start < values_to_set.size(); ++argv_start) {
        if (values_to_set[argv_start].contains('=')) {
            putenv(const_cast<char*>(values_to_set[argv_start].characters()));
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

    for (size_t i = argv_start; i < values_to_set.size(); ++i) {
        new_argv.append(values_to_set[i]);
    }

    if (new_argv.size() == 0) {
        for (auto entry = environ; *entry != nullptr; ++entry)
            outln("{}", *entry);

        return 0;
    }

    TRY(Core::System::exec(new_argv[0], new_argv, Core::System::SearchInPath::Yes));
    return 1;
}
