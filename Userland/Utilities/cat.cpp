/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 * Copyright (c) 2024, Perrin Smith <bobstlt40@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

struct LineTracker {
    size_t line_count { 1 };
    bool display_line_number { true };
};

static void output_buffer_with_line_numbers(LineTracker& line_tracker, ReadonlyBytes buffer_span)
{
    size_t span_index = 0;
    size_t span_index_of_last_write = 0;
    for (auto const curr_value : buffer_span) {
        if (line_tracker.display_line_number) {
            out("{:s}", buffer_span.slice(span_index_of_last_write, span_index - span_index_of_last_write));
            out("{: >6}\t", line_tracker.line_count);
            span_index_of_last_write = span_index;
            line_tracker.line_count++;
            line_tracker.display_line_number = false;
        }
        if (curr_value == '\n')
            line_tracker.display_line_number = true;
        span_index++;
    }

    if (span_index - span_index_of_last_write > 0)
        out("{:s}", buffer_span.slice(span_index_of_last_write));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
    bool show_lines = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(show_lines, "Number all output lines", "number", 'n');
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

    // used only if we are using the -n option
    LineTracker line_tracker;

    Array<u8, 32768> buffer;
    for (auto const& file : files) {
        while (!file->is_eof()) {
            auto const buffer_span = TRY(file->read_some(buffer));
            if (show_lines) {
                output_buffer_with_line_numbers(line_tracker, buffer_span);
            } else {
                out("{:s}", buffer_span);
            }
        }
    }

    return files.size() != paths.size();
}
