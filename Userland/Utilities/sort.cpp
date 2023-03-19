/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <ctype.h>

struct Line {
    StringView key;
    long int numeric_key;
    DeprecatedString line;
    bool numeric;

    bool operator<(Line const& other) const
    {
        if (numeric)
            return numeric_key < other.numeric_key;

        return key < other.key;
    }

    bool operator==(Line const& other) const
    {
        if (numeric)
            return numeric_key == other.numeric_key;

        return key == other.key;
    }

private:
};

template<>
struct AK::Traits<Line> : public GenericTraits<Line> {
    static unsigned hash(Line l)
    {
        if (l.numeric)
            return l.numeric_key;

        return l.key.hash();
    }
};

struct Options {
    size_t key_field { 0 };
    bool unique { false };
    bool numeric { false };
    bool reverse { false };
    StringView separator { "\0", 1 };
    Vector<DeprecatedString> files;
};

static Vector<Line> parse_lines(Options const& options, Vector<ByteBuffer> const& file_list)
{
    HashTable<Line> seen;
    Vector<Line> lines;

    for (auto const& file : file_list) {
        StringView file_string_view { file.bytes() };
        for (auto line : file_string_view.lines(true)) {
            StringView key = line;
            if (options.key_field != 0) {
                auto split = (options.separator[0])
                    ? line.split_view(options.separator[0])
                    : line.split_view_if(isspace);

                if (options.key_field - 1 >= split.size()) {
                    key = ""sv;
                } else {
                    key = split[options.key_field - 1];
                }
            }

            Line l = { key, key.to_int().value_or(0), line, options.numeric };

            if (!options.unique || !seen.contains(l)) {
                lines.append(l);
                if (options.unique)
                    seen.set(l);
            }
        }
    }

    return lines;
}

static ErrorOr<Vector<ByteBuffer>> read_files(Vector<StringView> const& file_paths)
{
    Vector<ByteBuffer> file_list;
    for (auto const& filename : file_paths) {
        auto file = TRY(Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read));
        auto file_buffer = TRY(file->read_until_eof());

        TRY(file_list.try_append(file_buffer));
    }

    return file_list;
}

ErrorOr<int> serenity_main([[maybe_unused]] Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Options options;

    Core::ArgsParser args_parser;
    args_parser.add_option(options.key_field, "The field to sort by", "key-field", 'k', "keydef");
    args_parser.add_option(options.unique, "Don't emit duplicate lines", "unique", 'u');
    args_parser.add_option(options.numeric, "treat the key field as a number", "numeric", 'n');
    args_parser.add_option(options.separator, "The separator to split fields by", "sep", 't', "char");
    args_parser.add_option(options.reverse, "Sort in reverse order", "reverse", 'r');
    args_parser.add_positional_argument(options.files, "Files to sort", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (options.files.is_empty())
        options.files.append("-"sv);

    auto file_list = TRY(read_files(options.files));
    auto lines = parse_lines(options, file_list);

    quick_sort(lines);

    auto print_lines = [](auto const& lines) {
        for (auto& line : lines)
            outln("{}", line.line);
    };

    if (options.reverse)
        print_lines(lines.in_reverse());
    else
        print_lines(lines);

    return 0;
}
