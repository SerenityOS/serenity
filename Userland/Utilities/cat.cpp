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

static void out_visible(unsigned char c)
{
    if (c == '\n') {
        // newline
        out("{:c}", c);
    } else if (c < 0x20) {
        // control character
        out("^{}", c + 0x40);
    } else if (c == 0x7F) {
        // delete character
        out("^?");
    } else if (c > 0x7F) {
        // high bit is set - mask it off
        out("M-");
        out_visible(c & 0x7F);
    } else {
        // normal ascii
        out("{:c}", c);
    }
}

static void output_buffer(LineTracker& line_tracker, ReadonlyBytes buffer_span, bool show_lines, bool show_non_printing_chars)
{
    for (auto const curr_value : buffer_span) {
        if (show_lines) {
            if (line_tracker.display_line_number) {
                out("{: >6}\t", line_tracker.line_count);
                line_tracker.line_count++;
                line_tracker.display_line_number = false;
            }
            if (curr_value == '\n')
                line_tracker.display_line_number = true;
        }
        if (show_non_printing_chars)
            out_visible(curr_value);
        else
            out("{:c}", curr_value);
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
    bool show_lines = false;
    bool show_non_printing_chars = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Concatenate files or pipes to stdout.");
    args_parser.add_positional_argument(paths, "File path", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(show_lines, "Number all output lines", "number", 'n');
    args_parser.add_option(show_non_printing_chars, "Display non-printing characters", "display", 'v');
    args_parser.parse(arguments);
    
    if (paths.is_empty())
        paths.append("-"sv);

    Vector<NonnullOwnPtr<Core::File>> files;
    TRY(files.try_ensure_capacity(paths.size()));

    for (auto const& path : paths) {
        auto result = Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read);
        if (result.is_error())
            warnln("Failed to open {}: {}", path, result.release_error());
        else
            files.unchecked_append(result.release_value());
    }

    TRY(Core::System::pledge("stdio"));

    LineTracker line_tracker;
    Array<u8, 32768> buffer;

    for (auto const& file : files) {
        while (!file->is_eof()) {
            auto const buffer_span = TRY(file->read_some(buffer));
            if (show_lines || show_non_printing_chars) {
                output_buffer(line_tracker, buffer_span, show_lines, show_non_printing_chars);
            } else {
                out("{:s}", buffer_span);
            }
        }
    }

    return files.size() != paths.size() ? 1 : 0;
}
