/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments argmuments)
{
    TRY(Core::System::pledge("stdio cpath"));

    bool force = false;
    bool symbolic = false;
    const char* target = nullptr;
    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(force, "Force the creation", "force", 'f');
    args_parser.add_option(symbolic, "Create a symlink", "symbolic", 's');
    args_parser.add_positional_argument(target, "Link target", "target");
    args_parser.add_positional_argument(path, "Link path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argmuments);

    String path_buffer;
    if (!path) {
        path_buffer = LexicalPath::basename(target);
        path = path_buffer.characters();
    }

    do {
        if (symbolic) {
            int rc = symlink(target, path);
            if (rc < 0 && !force) {
                perror("symlink");
                return 1;
            } else if (rc == 0) {
                return 0;
            }
        } else {
            int rc = link(target, path);
            if (rc < 0 && !force) {
                perror("link");
                return 1;
            } else if (rc == 0) {
                return 0;
            }
        }

        int rc = unlink(path);
        if (rc < 0) {
            perror("unlink");
            return 1;
        }
        force = false;
    } while (true);

    return 0;
}
