/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (paths.is_empty())
        paths.append("-"sv);

    Vector<NonnullOwnPtr<Core::File>> files;
    TRY(files.try_ensure_capacity(paths.size()));

    for (auto const& path : paths) {
        if (auto result = Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read); result.is_error())
            warnln("Failed to open {}: {}", path, result.release_error());
        else
            files.unchecked_append(result.release_value());
    }

    TRY(Core::System::pledge("stdio"));

    Array<u8, 32768> buffer;
    for (auto const& file : files) {
        while (!file->is_eof()) {
            auto const buffer_span = TRY(file->read_some(buffer));
            out("{:s}", buffer_span);
        }
    }

    return files.size() != paths.size();
}
