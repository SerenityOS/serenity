/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    DeprecatedString path = {};
    Core::ArgsParser args_parser;
    TRY(args_parser.add_positional_argument(path, "Path", "path"));
    TRY(args_parser.parse(arguments));

    outln("{}", LexicalPath::dirname(path));
    return 0;
}
