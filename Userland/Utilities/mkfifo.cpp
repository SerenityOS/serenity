/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/stat.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio dpath"));

    mode_t mode = 0666;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    // FIXME: add -m for file modes
    args_parser.add_positional_argument(paths, "Paths of FIFOs to create", "paths");
    args_parser.parse(arguments);

    int exit_code = 0;
    for (auto path : paths) {
        auto error_or_void = Core::System::mkfifo(path, mode);
        if (error_or_void.is_error()) {
            perror("mkfifo");
            exit_code = 1;
        }
    }

    return exit_code;
}
