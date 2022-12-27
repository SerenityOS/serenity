/*
 * Copyright (c) 2020, Matthew L. Curry <matthew.curry@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <unistd.h>

static ErrorOr<void> write_line_content(StringView line, size_t count, bool duplicates_only, bool print_count, Core::Stream::File& outfile)
{
    if (duplicates_only && count <= 1)
        return {};

    if (print_count)
        TRY(outfile.write(DeprecatedString::formatted("{} {}\n", count, line).bytes()));
    else
        TRY(outfile.write(DeprecatedString::formatted("{}\n", line).bytes()));
    return {};
}

static StringView skip(StringView line, unsigned char_skip_count, unsigned field_skip_count)
{
    line = line.trim("\n"sv);
    if (field_skip_count) {
        bool in_field = false;
        int field_index = 0;
        unsigned current_field = 0;
        for (size_t i = 0; i < line.length(); i++) {
            char c = line[i];
            if (is_ascii_space(c)) {
                in_field = false;
                field_index = i;
                if (++current_field > field_skip_count)
                    break;
            } else if (!in_field) {
                in_field = true;
            }
        }
        line = line.substring_view(field_index);
    }
    char_skip_count = min(char_skip_count, line.length());
    return line.substring_view(char_skip_count);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    StringView inpath;
    StringView outpath;
    bool duplicates_only = false;
    bool unique_only = false;
    bool ignore_case = false;
    bool print_count = false;
    unsigned skip_chars = 0;
    unsigned skip_fields = 0;

    Core::ArgsParser args_parser;
    args_parser.add_option(duplicates_only, "Only print duplicated lines", "repeated", 'd');
    args_parser.add_option(unique_only, "Only print unique lines (default)", "unique", 'u');
    args_parser.add_option(ignore_case, "Ignore case when comparing lines", "ignore-case", 'i');
    args_parser.add_option(print_count, "Prefix each line by its number of occurrences", "count", 'c');
    args_parser.add_option(skip_chars, "Skip N chars", "skip-chars", 's', "N");
    args_parser.add_option(skip_fields, "Skip N fields", "skip-fields", 'f', "N");
    args_parser.add_positional_argument(inpath, "Input file", "input", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(outpath, "Output file", "output", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!unique_only && !duplicates_only) {
        unique_only = true;
    } else if (unique_only && duplicates_only) {
        // Printing duplicated and unique lines shouldn't print anything
        return 0;
    }

    auto infile = TRY(Core::Stream::BufferedFile::create(TRY(Core::Stream::File::open_file_or_standard_stream(inpath, Core::Stream::OpenMode::Read))));
    auto outfile = TRY(Core::Stream::File::open_file_or_standard_stream(outpath, Core::Stream::OpenMode::Write));

    size_t count = 0;

    ByteBuffer first_buffer = TRY(ByteBuffer::create_uninitialized(1024));
    ByteBuffer second_buffer = TRY(ByteBuffer::create_uninitialized(1024));
    bool use_first_buffer = false;

    StringView previous = TRY(infile->read_line_unbounded(first_buffer));
    StringView previous_to_compare = skip(previous, skip_chars, skip_fields);

    while (!(infile->is_eof())) {
        auto& current_buf = use_first_buffer ? first_buffer : second_buffer;
        StringView current = TRY(infile->read_line_unbounded(current_buf));

        StringView current_to_compare = skip(current, skip_chars, skip_fields);
        bool lines_equal = ignore_case ? current_to_compare.equals_ignoring_case(previous_to_compare) : current_to_compare == previous_to_compare;
        if (!lines_equal) {
            TRY(write_line_content(previous, count, duplicates_only, print_count, *outfile));

            // Ensure we print out the end of a file that's missing a trailing newline.
            if (infile->is_eof() && !current.is_empty())
                TRY(write_line_content(current, 1, duplicates_only, print_count, *outfile));

            count = 1;
            previous = current;
            previous_to_compare = current_to_compare;
            use_first_buffer = !use_first_buffer;
        } else {
            count++;
        }
    }

    return 0;
}
