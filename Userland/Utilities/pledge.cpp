/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView promises;
    Vector<StringView> command;

    Core::ArgsParser args_parser;
    args_parser.add_option(promises, "Space-separated list of pledge promises", "promises", 'p', "promises");
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments);

    TRY(Core::System::pledge(StringView(), promises));
    TRY(Core::System::exec(command[0], command.span(), Core::System::SearchInPath::Yes));
    return 0;
}
