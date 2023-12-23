/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio dpath"));

    StringView mode_string { "0666"sv };
    StringView name;
    StringView type_string;
    StringView major_string;
    StringView minor_string;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Creates a file system node.");
    args_parser.add_option(mode_string, "File mode in octal or symbolic notation", "mode", 'm', "mode");
    args_parser.add_positional_argument(name, "Pathname to create", "name", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(
        type_string,
        "Type of file to create <b|c|u|p>"
        "\n\t\tb\tcreate a block special file"
        "\n\t\tc, u\tcreate a character special file"
        "\n\t\tp\tcreate a FIFO",
        "type",
        Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(major_string, "Major device number", "major", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(minor_string, "Minor device number", "minor", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto mask = TRY(Core::FilePermissionsMask::parse(mode_string));
    char type = type_string[0];

    mode_t mode;
    switch (type) {
    case 'c':
    case 'u':
        mode = mask.apply(S_IFCHR);
        break;
    case 'b':
        mode = mask.apply(S_IFBLK);
        break;
    case 'p':
        mode = mask.apply(S_IFIFO);
        break;
    default:
        warnln("Invalid device type {}", type);
        return 1;
    }

    auto maybe_major = major_string.to_number<int>();
    auto maybe_minor = minor_string.to_number<int>();
    dev_t device;
    if (type == 'p') {
        if (maybe_major.has_value() || maybe_minor.has_value()) {
            warnln("Do not set device numbers when creating FIFO");
            return 1;
        }
        device = makedev(0, 0);
    } else {
        if (!(maybe_major.has_value() && maybe_minor.has_value())) {
            warnln("Major and minor device numbers are required");
            return 1;
        }
        device = makedev(maybe_major.value(), maybe_minor.value());
    }

    TRY(Core::System::mknod(name, mode, device));

    return 0;
}
