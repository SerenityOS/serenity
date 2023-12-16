/*
 * Copyright (c) 2021-2022, Federico Guerinoni <guerinoni.federico@gmail.com>
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
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout, last line first.");
    args_parser.add_positional_argument(paths, "File path(s)", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Vector<FILE*> streams;
    auto num_paths = paths.size();
    streams.ensure_capacity(num_paths ? num_paths : 1);

    if (!paths.is_empty()) {
        for (auto const& path : paths) {
            FILE* stream = nullptr;
            if (path == "-"sv) {
                stream = stdin;
            } else {
                stream = fopen(ByteString(path).characters(), "r");
                if (!stream) {
                    warnln("Failed to open {}: {}", path, strerror(errno));
                    continue;
                }
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

    TRY(Core::System::pledge("stdio"));

    for (auto* stream : streams) {
        Vector<ByteString> lines;
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
            lines.append({ buffer, Chomp });
        }
        for (int i = lines.size() - 1; i >= 0; --i)
            outln("{}", lines[i]);
    }

    return 0;
}
