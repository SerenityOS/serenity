/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/stat.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio dpath"));

    ByteString mode_string;
    mode_t mask_reference_mode = 0777;
    mode_t mode = 0666;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(mode_string, "Set FIFO permissions", "mode", 'm', "mode");
    args_parser.add_positional_argument(paths, "Paths of FIFOs to create", "paths");
    args_parser.parse(arguments);

    if (!mode_string.is_empty()) {
        auto mask = TRY(Core::FilePermissionsMask::parse(mode_string));
        mode = mask.apply(mask_reference_mode);
    }

    int exit_code = 0;
    for (auto path : paths) {
        auto error_or_void = Core::System::mkfifo(path, mode);
        if (error_or_void.is_error()) {
            warnln("mkfifo: Couldn't create fifo '{}': {}", path, error_or_void.error());
            exit_code = 1;
        }
    }

    return exit_code;
}
