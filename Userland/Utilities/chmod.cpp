/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath fattr"));

    StringView mode;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(mode, "File mode in octal or symbolic notation", "mode");
    args_parser.add_positional_argument(paths, "Paths to file", "paths");
    args_parser.parse(arguments);

    auto mask = TRY(Core::FilePermissionsMask::parse(mode));

    for (auto const& path : paths) {
        auto current_access = TRY(Core::System::stat(path));
        TRY(Core::System::chmod(path, mask.apply(current_access.st_mode)));
    }

    return 0;
}
