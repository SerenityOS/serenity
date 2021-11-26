/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath", nullptr));

    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Vector<int> fds;
    if (paths.is_empty()) {
        TRY(fds.try_append(STDIN_FILENO));
    } else {
        for (auto const& path : paths) {
            int fd;
            if (path == "-") {
                fd = 0;
            } else {
                auto fd_or_error = Core::System::open(path, O_RDONLY);
                if (fd_or_error.is_error()) {
                    warnln("Failed to open {}: {}", path, fd_or_error.error());
                    continue;
                }
                fd = fd_or_error.release_value();
            }
            TRY(fds.try_append(fd));
        }
    }

    TRY(Core::System::pledge("stdio", nullptr));

    for (auto& fd : fds) {
        for (;;) {
            char buffer[32768];
            auto nread = TRY(Core::System::read(fd, buffer, sizeof(buffer)));
            if (nread == 0)
                break;
            ssize_t already_written = 0;
            while (already_written < nread)
                already_written += TRY(Core::System::write(STDOUT_FILENO, buffer + already_written, nread - already_written));
        }
        TRY(Core::System::close(fd));
    }

    return 0;
}
