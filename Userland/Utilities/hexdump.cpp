/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <string.h>

static constexpr size_t LINE_LENGTH_BYTES = 16;

enum class State {
    Print,
    PrintFiller,
    SkipPrint
};

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    StringView path;
    bool verbose = false;
    Optional<size_t> max_bytes;
    Optional<size_t> seek_to;

    args_parser.add_positional_argument(path, "Input", "input", Core::ArgsParser::Required::No);
    args_parser.add_option(verbose, "Display all input data", "verbose", 'v');
    args_parser.add_option(max_bytes, "Truncate to a fixed number of bytes", nullptr, 'n', "bytes");
    args_parser.add_option(seek_to, "Seek to a byte offset", "seek", 's', "offset");
    args_parser.parse(args);

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));
    if (seek_to.has_value())
        TRY(file->seek(seek_to.value(), SeekMode::SetPosition));

    auto print_line = [](Bytes line) {
        VERIFY(line.size() <= LINE_LENGTH_BYTES);
        for (size_t i = 0; i < LINE_LENGTH_BYTES; ++i) {
            if (i < line.size())
                out("{:02x} ", line[i]);
            else
                out("   ");

            if (i == 7)
                out("  ");
        }

        out("  |");

        for (auto const& byte : line) {
            if (is_ascii_printable(byte))
                putchar(byte);
            else
                putchar('.');
        }

        putchar('|');
        putchar('\n');
    };

    Array<u8, BUFSIZ> contents;
    Bytes bytes;
    Bytes previous_line;
    static_assert(LINE_LENGTH_BYTES * 2 <= contents.size(), "Buffer is too small?!");
    size_t total_bytes_read = 0;

    auto state = State::Print;
    bool is_input_remaining = true;
    while (is_input_remaining) {
        auto bytes_to_read = contents.size() - bytes.size();

        if (max_bytes.has_value()) {
            auto bytes_remaining = max_bytes.value() - total_bytes_read;
            if (bytes_remaining < bytes_to_read) {
                bytes_to_read = bytes_remaining;
                is_input_remaining = false;
            }
        }

        bytes = contents.span().slice(0, bytes_to_read);
        bytes = TRY(file->read_some(bytes));

        total_bytes_read += bytes.size();

        if (bytes.size() < bytes_to_read) {
            is_input_remaining = false;
        }

        while (bytes.size() > LINE_LENGTH_BYTES) {
            auto current_line = bytes.slice(0, LINE_LENGTH_BYTES);
            bytes = bytes.slice(LINE_LENGTH_BYTES);

            if (verbose) {
                print_line(current_line);
                continue;
            }

            bool is_same_contents = (current_line == previous_line);
            if (!is_same_contents)
                state = State::Print;
            else if (is_same_contents && (state != State::SkipPrint))
                state = State::PrintFiller;

            // Coalesce repeating lines
            switch (state) {
            case State::Print:
                print_line(current_line);
                break;
            case State::PrintFiller:
                outln("*");
                state = State::SkipPrint;
                break;
            case State::SkipPrint:
                break;
            }
            previous_line = current_line;
        }
    }

    if (bytes.size() > 0)
        print_line(bytes);

    return 0;
}
