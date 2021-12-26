/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    const char* group = nullptr;
    const char* key = nullptr;
    const char* value_to_write = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to INI file", "path");
    args_parser.add_positional_argument(group, "Group name", "group");
    args_parser.add_positional_argument(key, "Key name", "key");
    args_parser.add_positional_argument(value_to_write, "Value to write", "value", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!Core::File::exists(path)) {
        fprintf(stderr, "File does not exist: '%s'\n", path);
        return 1;
    }

    auto config = Core::ConfigFile::open(path);

    if (value_to_write) {
        config->write_entry(group, key, value_to_write);
        config->sync();
        return 0;
    }

    auto value = config->read_entry(group, key);
    if (!value.is_empty())
        printf("%s\n", value.characters());

    return 0;
}
