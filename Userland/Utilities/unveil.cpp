/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<ByteString> unveil_paths;
    Vector<StringView> command;

    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(unveil_paths, "Path to unveil [permissions,path]", "path", 'u', "");
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments);

    if (unveil_paths.is_empty()) {
        return Error::from_string_view("No unveil paths were specified."sv);
    }

    for (auto& path : unveil_paths) {
        auto parts = path.split_view(',');
        if (parts.size() != 2)
            return Error::from_string_literal("Unveil path being specified is invalid.");
        TRY(Core::System::unveil_after_exec(parts[1], parts[0]));
    }

    TRY(Core::System::exec(command[0], command, Core::System::SearchInPath::Yes));
    return 0;
}
