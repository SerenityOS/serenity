/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibRegex/Regex.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> files;

    bool recursive { false };
    bool use_ere { true };
    const char* pattern = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Recursively scan files starting in working directory", "recursive", 'r');
    args_parser.add_option(use_ere, "Extended regular expressions (default)", "extended-regexp", 'E');
    args_parser.add_option(pattern, "Pattern", "regexp", 'e', "Pattern");
    args_parser.add_positional_argument(files, "File(s) to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!use_ere)
        return 0;

    // mock grep behaviour: if -e is omitted, use first positional argument as pattern
    if (pattern == nullptr && files.size())
        pattern = files.take_first();

    Regex<PosixExtended> re(pattern);
    if (re.parser_result.error != Error::NoError) {
        return 1;
    }

    auto match = [&](const char* str, const char* filename = "", bool print_filename = false) {
        size_t last_printed_char_pos { 0 };
        auto result = re.match(str, PosixFlags::Global);
        if (result.success) {
            if (result.matches.size() && print_filename) {
                printf("\x1B[34m%s:\x1B[0m", filename);
            }

            for (auto& match : result.matches) {

                printf("%s\x1B[32m%s\x1B[0m",
                    String(&str[last_printed_char_pos], match.global_offset - last_printed_char_pos).characters(),
                    match.view.to_string().characters());
                last_printed_char_pos = match.global_offset + match.view.length();
            }
            printf("%s", String(&str[last_printed_char_pos], strlen(str) - last_printed_char_pos).characters());
        }
    };

    auto find_nul = [](const u8* str, size_t length) {
        for (size_t i = 0; i < length - 1; ++i)
            if (str[i] == 0) {
                return true;
            }

        return false;
    };
    auto handle_file = [&match, &find_nul](const char* filename, bool print_filename) -> bool {
        auto file = Core::File::construct(filename);
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Failed to open %s: %s\n", filename, file->error_string());
            return false;
        }

        u8 check_buf[1024];
        size_t bytes = file->read(check_buf, 1024);
        Utf8View view { StringView { check_buf, bytes } };

        if (!view.validate() || find_nul(check_buf, bytes)) {
            printf("Skipping binary file (%s)\n", filename);
            return true;
        }

        file->seek(0, Core::IODevice::SeekMode::SetPosition);

        while (file->can_read_line()) {
            auto line = file->read_line(1024);
            auto str = String { reinterpret_cast<const char*>(line.data()), line.size() };
            match(str.characters(), filename, print_filename);
        }
        return true;
    };

    auto add_directory = [&handle_file](String base, Optional<String> recursive, auto handle_directory) -> void {
        Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = it.next_full_path();
            if (!Core::File::is_directory(path)) {
                auto key = path.substring(base.length() + 1, path.length() - base.length() - 1);
                handle_file(key.characters(), true);
            } else {
                handle_directory(base, path, handle_directory);
            }
        }
    };

    if (!files.size() && !recursive) {
        bool first = { true };
        for (;;) {
            char buf[4096];
            auto* str = fgets(buf, sizeof(buf), stdin);

            if (first && strstr(str, "\0"))
                printf("Skipping binary file (stdin)\n");
            else
                match(str);

            first = false;

            if (feof(stdin))
                return 0;

            ASSERT(str);
        }
    } else {
        if (recursive) {
            add_directory(".", {}, add_directory);

        } else {
            bool print_filename { files.size() > 1 };
            for (auto& filename : files) {
                if (!handle_file(filename, print_filename))
                    return 1;
            }
        }
    }

    return 0;
}
