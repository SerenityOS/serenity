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
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

#ifdef __serenity
#    include <regex.h>
#else
#    include <LibC/regex.h>
#endif

#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
#ifdef __serenity__
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

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

    static constexpr int num_matches { 8 };
    regmatch_t matches[num_matches];
    regex_t regex;

    if (use_ere) {
        if (regcomp(&regex, pattern, REG_EXTENDED) != REG_NOERR) {
            fprintf(stderr, "Error in regular expression pattern.\n");
            return 1;
        }
    }

    if (!files.size()) {
        for (;;) {
            char buf[4096];
            auto* str = fgets(buf, sizeof(buf), stdin);

            size_t last_printed_char_pos { 0 };
            if (regexec(&regex, str, num_matches, matches, REG_MATCHALL) == REG_NOERR) {
                size_t n = 0;
                for (size_t i = 0; i < matches[0].match_count; ++i) {
                    printf("%s\x1B[32m%s\x1B[0m",
                        String(&str[last_printed_char_pos], matches[n].rm_so - last_printed_char_pos).characters(),
                        String(&str[matches[n].rm_so], matches[n].rm_eo - matches[n].rm_so).characters());
                    last_printed_char_pos = matches[n].rm_eo;
                    n += regex.re_nsub + 1;
                }
                printf("%s", String(&str[last_printed_char_pos], strlen(str) - last_printed_char_pos).characters());
            }

            if (feof(stdin)) {
                regfree(&regex);
                return 0;
            }
            ASSERT(str);
        }
    } else {
        if (recursive) {
            ASSERT_NOT_REACHED();
        } else {
            bool print_filename { files.size() > 1 };
            for (auto& filename : files) {
                auto file = Core::File::construct(filename);
                if (!file->open(Core::IODevice::ReadOnly)) {
                    fprintf(stderr, "Failed to open %s: %s\n", filename, file->error_string());
                    regfree(&regex);
                    return 1;
                }

                while (file->can_read_line()) {
                    auto line = file->read_line(1024);
                    auto str = String { reinterpret_cast<const char*>(line.data()), line.size() };
                    size_t last_printed_char_pos { 0 };

                    if (regexec(&regex, str.characters(), num_matches, matches, REG_MATCHALL) == REG_NOERR) {
                        size_t n = 0;
                        for (size_t i = 0; i < matches[0].match_count; ++i) {
                            printf("%s%s%s\x1B[32m%s\x1B[0m",
                                print_filename ? filename : "",
                                print_filename ? ": " : "",
                                String(&str.characters()[last_printed_char_pos], matches[n].rm_so - last_printed_char_pos).characters(),
                                String(&str.characters()[matches[n].rm_so], matches[n].rm_eo - matches[n].rm_so).characters());
                            last_printed_char_pos = matches[n].rm_eo;
                            n += regex.re_nsub + 1;
                        }
                        printf("%s", String(&str.characters()[last_printed_char_pos], str.length() - last_printed_char_pos).characters());
                    }
                }
            }
        }
    }

    regfree(&regex);

    return 0;
}
