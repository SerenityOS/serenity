/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Vector<NonnullRefPtr<Core::File>> files;
    if (paths.is_empty()) {
        TRY(files.try_append(Core::File::standard_input()));
    } else {
        TRY(files.try_ensure_capacity(paths.size()));
        for (auto const& path : paths) {
            if (path == "-") {
                files.unchecked_append(Core::File::standard_input());
            } else {
                auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
                if (file_or_error.is_error())
                    warnln("Failed to open {}: {}", path, file_or_error.error());
                else
                    files.unchecked_append(file_or_error.release_value());
            }
        }
    }

    TRY(Core::System::pledge("stdio"));

    for (auto& file : files) {
        TRY(file->try_read_all_chunked([](auto chunk) {
            out("{}", StringView(chunk));
        }));
    }

    return 0;
}
