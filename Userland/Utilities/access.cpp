/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdint.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView path;
    Core::ArgsParser parser;
    bool readonly = false;
    bool dont_exit_on_success = false;
    parser.set_general_help("Open a file in a filesystem in the intention to keep the mount busy.");
    parser.add_option(readonly, "Open in read-only mode", "readonly", 'r');
    parser.add_option(dont_exit_on_success, "Don't exit on success", "keep-access", 's');
    parser.add_positional_argument(path, "file path to open", "path");
    parser.parse(arguments);

    int fd = -1;
    if (readonly)
        fd = TRY(Core::System::open(path, O_RDONLY));
    else
        fd = TRY(Core::System::open(path, O_RDWR));

    (void)fd;

    if (!dont_exit_on_success)
        return 0;

    while (true) {
    }

    return 0;
}
