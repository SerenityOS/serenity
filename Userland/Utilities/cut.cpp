/*
 * Copyright (c) 2019-2020, Marios Prokopakis <mariosprokopakis@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>

struct Range {
    size_t m_from { 1 };
    size_t m_to { SIZE_MAX };

    [[nodiscard]] bool intersects(Range const& other) const
    {
        return !(other.m_from > m_to || other.m_to < m_from);
    }

    void merge(Range const& other)
    {
        // Can't merge two ranges that are disjoint.
        VERIFY(intersects(other));

        m_from = min(m_from, other.m_from);
        m_to = max(m_to, other.m_to);
    }

    bool contains(size_t x) const
    {
        return m_from <= x && m_to >= x;
    }
};

static bool expand_list(ByteString& list, Vector<Range>& ranges)
{
    Vector<ByteString> tokens = list.split(',', SplitBehavior::KeepEmpty);

    for (auto& token : tokens) {
        if (token.length() == 0) {
            warnln("cut: byte/character positions are numbered from 1");
            return false;
        }

        if (token == "-") {
            warnln("cut: invalid range with no endpoint: {}", token);
            return false;
        }

        if (token[0] == '-') {
            auto index = token.substring(1, token.length() - 1).to_number<unsigned>();
            if (!index.has_value()) {
                warnln("cut: invalid byte/character position '{}'", token);
                return false;
            }

            if (index.value() == 0) {
                warnln("cut: byte/character positions are numbered from 1");
                return false;
            }

            ranges.append({ 1, index.value() });
        } else if (token[token.length() - 1] == '-') {
            auto index = token.substring(0, token.length() - 1).to_number<unsigned>();
            if (!index.has_value()) {
                warnln("cut: invalid byte/character position '{}'", token);
                return false;
            }

            if (index.value() == 0) {
                warnln("cut: byte/character positions are numbered from 1");
                return false;
            }

            ranges.append({ index.value(), SIZE_MAX });
        } else {
            auto range = token.split('-', SplitBehavior::KeepEmpty);
            if (range.size() == 2) {
                auto index1 = range[0].to_number<unsigned>();
                if (!index1.has_value()) {
                    warnln("cut: invalid byte/character position '{}'", range[0]);
                    return false;
                }

                auto index2 = range[1].to_number<unsigned>();
                if (!index2.has_value()) {
                    warnln("cut: invalid byte/character position '{}'", range[1]);
                    return false;
                }

                if (index1.value() > index2.value()) {
                    warnln("cut: invalid decreasing range");
                    return false;
                } else if (index1.value() == 0 || index2.value() == 0) {
                    warnln("cut: byte/character positions are numbered from 1");
                    return false;
                }

                ranges.append({ index1.value(), index2.value() });
            } else if (range.size() == 1) {
                auto index = range[0].to_number<unsigned>();
                if (!index.has_value()) {
                    warnln("cut: invalid byte/character position '{}'", range[0]);
                    return false;
                }

                if (index.value() == 0) {
                    warnln("cut: byte/character positions are numbered from 1");
                    return false;
                }

                ranges.append({ index.value(), index.value() });
            } else {
                warnln("cut: invalid byte or character range");
                return false;
            }
        }
    }

    return true;
}

static void process_line_bytes(StringView line, Vector<Range> const& ranges)
{
    for (auto& i : ranges) {
        if (i.m_from > line.length())
            continue;

        auto to = min(i.m_to, line.length());
        auto sub_string = ByteString(line).substring(i.m_from - 1, to - i.m_from + 1);
        out("{}", sub_string);
    }
    outln();
}

static ErrorOr<void> process_line_characters(StringView line, Vector<Range> const& ranges)
{
    for (auto const& range : ranges) {
        if (range.m_from > line.length())
            continue;

        auto s = TRY(String::from_utf8(line));
        size_t i = 1;
        for (auto c : s.code_points()) {
            if (range.contains(i++))
                out("{}", String::from_code_point(c));
        }
    }
    outln();
    return {};
}

static void process_line_fields(StringView line, Vector<Range> const& ranges, char delimiter, bool only_print_delimited_lines)
{
    auto string_split = ByteString(line).split(delimiter, SplitBehavior::KeepEmpty);
    if (string_split.size() == 1) {
        if (!only_print_delimited_lines)
            outln("{}", line);

        return;
    }

    Vector<ByteString> output_fields;
    for (auto& range : ranges) {
        for (size_t i = range.m_from - 1; i < min(range.m_to, string_split.size()); i++) {
            output_fields.append(string_split[i]);
        }
    }

    outln("{}", ByteString::join(delimiter, output_fields));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    ByteString byte_list = "";
    ByteString character_list = "";
    ByteString fields_list = "";
    ByteString delimiter = "\t";
    bool only_print_delimited_lines = false;

    Vector<StringView> files;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(files, "file(s) to cut", "file", Core::ArgsParser::Required::No);
    args_parser.add_option(byte_list, "select only these bytes", "bytes", 'b', "list");
    args_parser.add_option(character_list, "select only these characters", "characters", 'c', "list");
    args_parser.add_option(fields_list, "select only these fields", "fields", 'f', "list");
    args_parser.add_option(delimiter, "set a custom delimiter", "delimiter", 'd', "delimiter");
    args_parser.add_option(only_print_delimited_lines, "suppress lines which don't contain any field delimiter characters", "only-delimited", 's');
    args_parser.parse(arguments);

    bool const selected_bytes = (byte_list != "");
    bool const selected_characters = (character_list != "");
    bool const selected_fields = (fields_list != "");

    int const selected_options_count = (selected_bytes ? 1 : 0) + (selected_characters ? 1 : 0) + (selected_fields ? 1 : 0);

    if (selected_options_count == 0) {
        warnln("cut: you must specify a list of bytes, characters, or fields");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (selected_options_count > 1) {
        warnln("cut: you must specify only one of bytes, characters, or fields");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (delimiter.length() != 1) {
        warnln("cut: the delimiter must be a single character");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    ByteString ranges_list;
    Vector<Range> ranges_vector;

    if (selected_bytes) {
        ranges_list = byte_list;
    } else if (selected_characters) {
        ranges_list = character_list;
    } else if (selected_fields) {
        ranges_list = fields_list;
    } else {
        // This should never happen, since we already checked the options count above.
        VERIFY_NOT_REACHED();
    }

    auto expansion_successful = expand_list(ranges_list, ranges_vector);

    if (!expansion_successful) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    quick_sort(ranges_vector, [](auto& a, auto& b) { return a.m_from < b.m_from; });

    Vector<Range> disjoint_ranges;
    for (auto& range : ranges_vector) {
        if (disjoint_ranges.is_empty()) {
            disjoint_ranges.append(range);
            continue;
        }

        Range& last_range = disjoint_ranges.last();

        if (!last_range.intersects(range)) {
            disjoint_ranges.append(range);
            continue;
        }

        last_range.merge(range);
    }

    if (files.is_empty())
        files.append(""sv);

    /* Process each file */
    for (auto const filename : files) {
        auto maybe_file = Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read);
        if (maybe_file.is_error()) {
            warnln("cut: Could not open file '{}'", filename.is_empty() ? "stdin"sv : filename);
            continue;
        }
        auto file = TRY(Core::InputBufferedFile::create(maybe_file.release_value()));

        auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
        while (!file->is_eof()) {
            auto line = TRY(file->read_line_with_resize(buffer));
            if (line.is_empty() && file->is_eof())
                break;

            if (selected_bytes) {
                process_line_bytes(line, disjoint_ranges);
            } else if (selected_characters) {
                TRY(process_line_characters(line, disjoint_ranges));
            } else if (selected_fields) {
                process_line_fields(line, disjoint_ranges, delimiter[0], only_print_delimited_lines);
            } else {
                VERIFY_NOT_REACHED();
            }
        }
    }

    return 0;
}
