/*
 * Copyright (c) 2021, Thomas Voss <thomasvoss@live.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"sv));

    Vector<StringView> paths;
    Core::ArgsParser args_parser;

    args_parser.set_general_help("Concatente files to stdout with each line in reverse.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Vector<FILE*> streams;
    auto num_paths = paths.size();
    streams.ensure_capacity(num_paths ? num_paths : 1);

    if (!paths.is_empty()) {
        for (auto const& path : paths) {
            FILE* stream = fopen(String(path).characters(), "r");
            if (!stream) {
                warnln("Failed to open {}: {}", path, strerror(errno));
                continue;
            }
            streams.append(stream);
        }
    } else {
        streams.append(stdin);
    }

    char* buffer = nullptr;
    ScopeGuard guard = [&] {
        free(buffer);
        for (auto* stream : streams) {
            if (fclose(stream))
                perror("fclose");
        }
    };

    TRY(Core::System::pledge("stdio"sv));

    for (auto* stream : streams) {
        for (;;) {
            size_t n = 0;
            errno = 0;
            ssize_t buflen = getline(&buffer, &n, stream);
            if (buflen == -1) {
                if (errno != 0) {
                    perror("getline");
                    return 1;
                }
                break;
            }
            outln("{}", String { buffer, Chomp }.reverse());
        }
    }

    return 0;
}
