/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    const char* path = nullptr;
    const char* group = nullptr;
    const char* key = nullptr;
    const char* value_to_write = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to INI file", "path");
    args_parser.add_positional_argument(group, "Group name", "group");
    args_parser.add_positional_argument(key, "Key name", "key");
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!Core::File::exists(path)) {
        warnln("File does not exist: '{}'", path);
        return 1;
    }

    auto config = Core::ConfigFile::open(path, value_to_write ? Core::ConfigFile::AllowWriting::Yes : Core::ConfigFile::AllowWriting::No);

    if (value_to_write) {
        config->write_entry(group, key, value_to_write);
        config->sync();
        return 0;
    }

    auto value = config->read_entry(group, key);
    if (!value.is_empty())
        outln("{}", value);

    return 0;
}
