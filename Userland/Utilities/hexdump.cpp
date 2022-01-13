/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
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
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    bool verbose = false;
    args_parser.add_positional_argument(path, "Input", "input", Core::ArgsParser::Required::No);
    args_parser.add_option(verbose, "Display all input data", "verbose", 'v');

    args_parser.parse(args);

    RefPtr<Core::File> file;

    if (!path)
        file = Core::File::standard_input();
    else
        file = TRY(Core::File::open(path, Core::OpenMode::ReadOnly));

    auto print_line = [](u8* buf, size_t size) {
        VERIFY(size <= LINE_LENGTH_BYTES);
        for (size_t i = 0; i < LINE_LENGTH_BYTES; ++i) {
            if (i < size)
                out("{:02x} ", buf[i]);
            else
                out("   ");

            if (i == 7)
                out("  ");
        }

        out("  |");

        for (size_t i = 0; i < size; ++i) {
            if (isprint(buf[i]))
                putchar(buf[i]);
            else
                putchar('.');
        }

        putchar('|');
        putchar('\n');
    };

    Array<u8, BUFSIZ> contents;
    Span<u8> previous_line;
    static_assert(LINE_LENGTH_BYTES * 2 <= contents.size(), "Buffer is too small?!");
    size_t contents_size = 0;

    int nread;
    auto state = State::Print;
    while (true) {
        nread = file->read(&contents[contents_size], BUFSIZ - contents_size);
        if (nread <= 0)
            break;
        contents_size += nread;

        size_t offset;
        for (offset = 0; offset + LINE_LENGTH_BYTES - 1 < contents_size; offset += LINE_LENGTH_BYTES) {
            if (verbose) {
                print_line(&contents[offset], LINE_LENGTH_BYTES);
                continue;
            }

            auto current_line = contents.span().slice(offset, LINE_LENGTH_BYTES);
            bool is_same_contents = (current_line == previous_line);
            if (!is_same_contents)
                state = State::Print;
            else if (is_same_contents && (state != State::SkipPrint))
                state = State::PrintFiller;

            // Coalesce repeating lines
            switch (state) {
            case State::Print:
                print_line(&contents[offset], LINE_LENGTH_BYTES);
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

        contents_size -= offset;
        VERIFY(contents_size < LINE_LENGTH_BYTES);
        // If we managed to make the buffer exactly full, &contents[BUFSIZ] would blow up.
        if (contents_size > 0) {
            // Regions cannot overlap due to above static_assert.
            memcpy(&contents[0], &contents[offset], contents_size);
        }
    }
    VERIFY(contents_size <= LINE_LENGTH_BYTES - 1);
    if (contents_size > 0)
        print_line(&contents[0], contents_size);

    return 0;
}
