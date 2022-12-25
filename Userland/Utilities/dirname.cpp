/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // FIXME: Remove this once we correctly define a proper set of pledge promises
    // (and if "exec" promise is not one of them).
    TRY(Core::System::prctl(PR_SET_NO_NEW_PRIVS, NO_NEW_PRIVS_MODE_ENFORCED, 0, 0));

    DeprecatedString path = {};
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path", "path");
    args_parser.parse(arguments);

    outln("{}", LexicalPath::dirname(path));
    return 0;
}
