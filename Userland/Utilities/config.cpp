/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibMain/Main.h>

static void print_group(StringView domain, StringView group)
{
    auto keys = Config::Client::the().list_keys(domain, group);
    quick_sort(keys);
    for (auto const& key : keys) {
        auto value = Config::Client::the().read_string_value(domain, group, key);
        if (!value.has_value())
            warnln("Can't find a value for {}:{}:{}", domain, group, key);
        else
            outln("{}={}", key, *value);
    }
}

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
    args_parser.add_positional_argument(group, "Group name", "group", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(key, "Key name", "key", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (remove) {
        if (group.is_empty())
            return Error::from_string_literal("Can't delete a domain");
        if (key.is_empty())
            Config::remove_group(domain, group);
        else
            Config::remove_key(domain, group, key);
        return 0;
    }

    if (!group.is_empty() && !key.is_empty() && !value_to_write.is_empty()) {
        Config::write_string(domain, group, key, value_to_write);
        return 0;
    }

    if (group.is_empty()) {
        auto groups = Config::Client::the().list_groups(domain);
        quick_sort(groups);

        for (auto const& group : groups) {
            outln("[{}]", group);
            print_group(domain, group);
        }
        return 0;
    }

    if (key.is_empty()) {
        print_group(domain, group);
        return 0;
    }

    auto value_or_error = Config::Client::the().read_string_value(domain, group, key);
    if (!value_or_error.has_value())
        return 1;
    outln("{}", value_or_error.value());
    return 0;
}
