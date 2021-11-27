/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop loop;
    String domain;
    String group;
    String key;
    String value_to_write;
    bool remove_key = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show or modify values in the configuration files through ConfigServer.");
    args_parser.add_option(remove_key, "Remove key", "remove", 'r');
    args_parser.add_positional_argument(domain, "Config domain", "domain");
    args_parser.add_positional_argument(group, "Group name", "group");
    args_parser.add_positional_argument(key, "Key name", "key");
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (remove_key) {
        Config::remove_key(domain, group, key);
        return 0;
    }

    if (!value_to_write.is_empty()) {
        Config::write_string(domain, group, key, value_to_write);
        return 0;
    }

    auto value_or_error = Config::Client::the().read_string_value(domain, group, key);
    if (!value_or_error.has_value())
        return 1;
    outln("{}", value_or_error.value());
    return 0;
}
