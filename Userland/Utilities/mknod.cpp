/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio dpath"));

    StringView name;
    StringView type_string;
    StringView major_string;
    StringView minor_string;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Creates a file system node.");
    args_parser.add_positional_argument(name, "Pathname to create", "name", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(type_string, "Type of file to create", "type", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(major_string, "Major device number", "major", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(minor_string, "Minor device number", "minor", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    // FIXME: Add some kind of option for specifying the file permissions.

    mode_t mode = 0666;
    char type = type_string[0];

    switch (type) {
    case 'c':
    case 'u':
        mode |= S_IFCHR;
        break;
    case 'b':
        mode |= S_IFBLK;
        break;
    case 'p':
        mode |= S_IFIFO;
        break;
    default:
        warnln("Invalid device type {}", type);
        return 1;
    }

    if (type == 'p') {
        if (!major_string.is_empty() || !minor_string.is_empty()) {
            warnln("Do not set device numbers when creating FIFO");
            return 1;
        }
    } else {
        if (major_string.is_empty() || minor_string.is_empty()) {
            warnln("Major and minor device numbers are required");
            return 1;
        }
    }

    int major = 0;
    int minor = 0;
    if (type != 'p') {
        major = atoi(major_string.characters_without_null_termination());
        minor = atoi(minor_string.characters_without_null_termination());
    }

    TRY(Core::System::mknod(name, mode, makedev(major, minor)));

    return 0;
}
