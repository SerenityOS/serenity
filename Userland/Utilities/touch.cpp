/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <utime.h>

static bool file_exists(const char* path)
{
    struct stat st;
    int rc = stat(path, &st);
    if (rc < 0) {
        if (errno == ENOENT)
            return false;
    }
    if (rc == 0) {
        return true;
    }
    perror("stat");
    exit(1);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath cpath fattr"));

    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Create a file, or update its mtime (time of last modification).");
    args_parser.add_ignored(nullptr, 'f');
    args_parser.add_positional_argument(paths, "Files to touch", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    for (auto path : paths) {
        if (file_exists(path)) {
            if (auto result = Core::System::utime(path, {}); result.is_error())
                warnln("{}", result.release_error());
        } else {
            int fd = TRY(Core::System::open(path, O_CREAT, 0100644));
            TRY(Core::System::close(fd));
        }
    }
    return 0;
}
