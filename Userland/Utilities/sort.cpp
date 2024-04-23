/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

struct Line {
    StringView key;
    long int numeric_key;
    ByteString line;
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
struct AK::Traits<Line> : public DefaultTraits<Line> {
    static unsigned hash(Line const& l)
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
    bool zero_terminated { false };
    StringView separator {};
    Vector<ByteString> files;
};

static ErrorOr<void> load_file(Options const& options, StringView filename, StringView line_delimiter, Vector<Line>& lines, HashTable<Line>& seen)
{
    auto file = TRY(Core::InputBufferedFile::create(
        TRY(Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read))));

    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (!file->is_eof()) {
        ByteString line { TRY(file->read_until_with_resize(buffer, line_delimiter)) };
        // Ensure any trailing delimiter is ignored.
        if (line.is_empty() && file->is_eof())
            break;

        StringView key = line;
        if (options.key_field != 0) {
            auto split = (!options.separator.is_empty())
                ? key.split_view(options.separator)
                : key.split_view_if(is_ascii_space);
            if (options.key_field - 1 >= split.size()) {
                key = ""sv;
            } else {
                key = split[options.key_field - 1];
            }
        }

        Line l = { key, key.to_number<int>().value_or(0), line, options.numeric };

        if (!options.unique || !seen.contains(l)) {
            lines.append(l);
            if (options.unique)
                seen.set(l);
        }
    }

    return {};
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
    args_parser.add_option(options.zero_terminated, "Use '\\0' as the line delimiter instead of a newline", "zero-terminated", 'z');
    args_parser.add_positional_argument(options.files, "Files to sort", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto line_delimiter = options.zero_terminated ? "\0"sv : "\n"sv;
    Vector<Line> lines;
    HashTable<Line> seen;

    if (options.files.size() == 0) {
        TRY(load_file(options, "-"sv, line_delimiter, lines, seen));
    } else {
        for (auto& file : options.files) {
            TRY(load_file(options, file, line_delimiter, lines, seen));
        }
    }

    quick_sort(lines);

    auto print_lines = [line_delimiter](auto const& lines) {
        for (auto& line : lines)
            out("{}{}", line.line, line_delimiter);
    };

    if (options.reverse)
        print_lines(lines.in_reverse());
    else
        print_lines(lines);

    return 0;
}
