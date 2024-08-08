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

struct LineTracker {
    size_t line_count = 1;
    bool display_line_number = true;
};

static void output_buffer_with_line_numbers(LineTracker& line_tracker, ReadonlyBytes buffer_span, bool show_only_non_blank_lines = false)
{
    if (buffer_span.size() == 0) {
        return;
    }

    size_t i = 0;

    // Handle special case where file starts with newlines
    if (show_only_non_blank_lines) {
        while (i < buffer_span.size() && buffer_span[i] == '\n') {
            out("{:c}", buffer_span[i]);
            i += 1;
        }
    }

    while (i < buffer_span.size()) {
        if (line_tracker.display_line_number) {
            out("{: >6}\t", line_tracker.line_count);
            line_tracker.line_count++;
            line_tracker.display_line_number = false;
        }
        if (buffer_span[i] == '\n') {
            if (show_only_non_blank_lines) {
                // Suck up all the newlines until we're looking at the last newline.
                //
                // Then let the code outside of this for loop process the current
                // character so we drop out if we're on the last character OR we're
                // at the last newline.
                while (i < buffer_span.size()) {
                    if (i == buffer_span.size() - 1) {
                        // Last character - print it out and be done
                        out("{:c}", buffer_span[i]);
                        return;
                    }
                    if (buffer_span[i + 1] != '\n') {
                        // We're at the last newline
                        break;
                    }
                    // Still more newlines to go
                    out("{:c}", buffer_span[i]);
                    i += 1;
                }
            }
            line_tracker.display_line_number = true;
        }
        out("{:c}", buffer_span[i]);
        i += 1;
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
    bool show_lines = false;
    bool show_only_non_blank_lines = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(show_lines, "Number all output lines", "number", 'n');
    args_parser.add_option(show_only_non_blank_lines, "Number all non-blank output lines", "number-non-blank", 'b');
    args_parser.parse(arguments);

    dbgln("show_only_non_blank_lines: {}", show_only_non_blank_lines);

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

    // used only if we are using the -n or -b option
    LineTracker line_tracker;

    Array<u8, 32768> buffer;
    for (auto const& file : files) {
        while (!file->is_eof()) {
            auto const buffer_span = TRY(file->read_some(buffer));
            if (show_lines || show_only_non_blank_lines) {
                output_buffer_with_line_numbers(line_tracker, buffer_span, show_only_non_blank_lines);
            } else {
                out("{:s}", buffer_span);
            }
        }
    }

    dbgln("files.size, paths.size: {}, {}", files.size(), paths.size());
    return files.size() != paths.size();
}
