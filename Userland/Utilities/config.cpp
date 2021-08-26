/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>

int main(int argc, char** argv)
{
    Core::EventLoop loop;
    String domain;
    String group;
    String key;
    String value_to_write;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show or modify values in the configuration files through ConfigServer.");
    args_parser.add_positional_argument(domain, "Config domain", "domain");
    args_parser.add_positional_argument(group, "Group name", "group");
    args_parser.add_positional_argument(key, "Key name", "key");
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!value_to_write.is_empty()) {
        Config::write_string(domain, group, key, value_to_write);
        return 0;
    }

    auto value = Config::read_string(domain, group, key);
    outln("{}", value);

    return 0;
}
