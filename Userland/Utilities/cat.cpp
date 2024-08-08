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
    enum state : u8 {
        LINE,
        NEWLINES,
    } state
        = NEWLINES;
};

static void output_buffer_with_line_numbers(LineTracker& line_tracker, ReadonlyBytes buffer_span, bool show_lines = true)
{
    for (size_t i = 0; i < buffer_span.size(); i++) {
        if (line_tracker.state == LineTracker::state::LINE) {
            if (buffer_span[i] == '\n') {
                out("{:c}", buffer_span[i]);
                line_tracker.state = LineTracker::state::NEWLINES;
            } else {
                out("{:c}", buffer_span[i]);
            }
        } else if (line_tracker.state == LineTracker::state::NEWLINES) {
            if (buffer_span[i] == '\n') {
                if (show_lines) {
                    out("{: >6}\t", line_tracker.line_count);
                    line_tracker.line_count++;
                }
                out("{:c}", buffer_span[i]);
            } else {
                out("{: >6}\t", line_tracker.line_count);
                line_tracker.line_count++;
                out("{:c}", buffer_span[i]);
                line_tracker.state = LineTracker::state::LINE;
            }
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
    bool show_lines = false;
    bool show_only_blank_lines = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(show_lines, "Number all output lines", "number", 'n');
    args_parser.add_option(show_only_blank_lines, "Number all non-blank output lines", "number-non-blank", 'b');
    args_parser.parse(arguments);

    if (show_lines && show_only_blank_lines) {
        warnln("cat: Cannot pass both -n and -b");
        return EINVAL;
    }

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
            if (show_lines || show_only_blank_lines) {
                output_buffer_with_line_numbers(line_tracker, buffer_span, show_lines);
            } else {
                out("{:s}", buffer_span);
            }
        }
    }

    return files.size() != paths.size();
}
