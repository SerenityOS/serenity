/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    StringView path;
    ByteString group;
    ByteString key;
    StringView value_to_write;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to INI file", "path");
    args_parser.add_positional_argument(group, "Group name", "group");
    args_parser.add_positional_argument(key, "Key name", "key");
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!FileSystem::exists(path)) {
        warnln("File does not exist: '{}'", path);
        return 1;
    }

    auto config = TRY(Core::ConfigFile::open(path, value_to_write.is_null() ? Core::ConfigFile::AllowWriting::No : Core::ConfigFile::AllowWriting::Yes));

    if (!value_to_write.is_null()) {
        config->write_entry(group, key, value_to_write);
        TRY(config->sync());
        return 0;
    }

    auto value = config->read_entry(group, key);
    if (!value.is_empty())
        outln("{}", value);

    return 0;
}
