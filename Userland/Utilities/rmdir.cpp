/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath"));

    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(paths, "Directories to remove", "paths");
    args_parser.parse(arguments);

    int status = 0;
    for (auto path : paths) {
        int rc = rmdir(path);
        if (rc < 0) {
            perror("rmdir");
            status = 1;
        }
    }
    return status;
}
