/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <ctype.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    args_parser.add_positional_argument(path, "Input", "input", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    RefPtr<Core::File> file;

    if (!path) {
        file = Core::File::standard_input();
    } else {
        auto file_or_error = Core::File::open(path, Core::File::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.error());
            return 1;
        }
        file = file_or_error.value();
    }

    auto contents = file->read_all();

    Vector<u8, 16> line;

    auto print_line = [&] {
        for (size_t i = 0; i < 16; ++i) {
            if (i < line.size())
                printf("%02x ", line[i]);
            else
                printf("   ");

            if (i == 7)
                printf("  ");
        }

        printf("  ");

        for (size_t i = 0; i < 16; ++i) {
            if (i < line.size() && isprint(line[i]))
                putchar(line[i]);
            else
                putchar(' ');
        }

        putchar('\n');
    };

    for (size_t i = 0; i < contents.size(); ++i) {
        line.append(contents[i]);

        if (line.size() == 16) {
            print_line();
            line.clear();
        }
    }

    if (!line.is_empty())
        print_line();

    return 0;
}
