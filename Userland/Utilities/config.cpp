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
    StringView domain;
    StringView group;
    StringView key;
    StringView value_to_write;
    bool remove = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show or modify values in the configuration files through ConfigServer.");
    args_parser.add_option(remove, "Remove group or key", "remove", 'r');
    args_parser.add_positional_argument(domain, "Config domain", "domain");
    args_parser.add_positional_argument(group, "Group name", "group");
    args_parser.add_positional_argument(key, "Key name", "key", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (remove) {
        if (!key.is_empty())
            Config::remove_key(domain, group, key);
        else
            Config::remove_group(domain, group);
        return 0;
    }

    if (key.is_empty() && value_to_write.is_empty()) {
        Config::add_group(domain, group);
        return 0;
    }

    if (!key.is_empty() && !value_to_write.is_empty()) {
        Config::write_string(domain, group, key, value_to_write);
        return 0;
    }

    auto value_or_error = Config::Client::the().read_string_value(domain, group, key);
    if (!value_or_error.has_value())
        return 1;
    outln("{}", value_or_error.value());
    return 0;
}
