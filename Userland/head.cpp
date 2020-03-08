/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StdLibExtras.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int head(const String& filename, bool print_filename, int line_count, int char_count);

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    int line_count = 0;
    int char_count = 0;
    bool never_print_filenames = false;
    bool always_print_filenames = false;
    Vector<const char*> files;

    Core::ArgsParser args_parser;
    args_parser.add_option(line_count, "Number of lines to print (default 10)", "lines", 'n', "number");
    args_parser.add_option(char_count, "Number of characters to print", "characters", 'c', "number");
    args_parser.add_option(never_print_filenames, "Never print file names", "quiet", 'q');
    args_parser.add_option(always_print_filenames, "Always print file names", "verbose", 'v');
    args_parser.add_positional_argument(files, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (line_count == 0 && char_count == 0) {
        line_count = 10;
    }

    bool print_filenames = files.size() > 1;
    if (always_print_filenames)
        print_filenames = true;
    else if (never_print_filenames)
        print_filenames = false;

    if (files.is_empty()) {
        return head("", print_filenames, line_count, char_count);
    }

    int rc = 0;

    for (auto& file : files) {
        if (head(file, print_filenames, line_count, char_count) != 0) {
            rc = 1;
        }
    }

    return rc;
}

int head(const String& filename, bool print_filename, int line_count, int char_count)
{
    bool is_stdin = false;
    FILE* fp = nullptr;

    if (filename == "" || filename == "-") {
        fp = stdin;
        is_stdin = true;
    } else {
        fp = fopen(filename.characters(), "r");
        if (!fp) {
            fprintf(stderr, "can't open %s for reading: %s\n", filename.characters(), strerror(errno));
            return 1;
        }
    }

    if (print_filename) {
        if (is_stdin) {
            puts("==> standard input <==");
        } else {
            printf("==> %s <==\n", filename.characters());
        }
    }

    if (line_count) {
        for (int line = 0; line < line_count; ++line) {
            char buffer[BUFSIZ];
            auto* str = fgets(buffer, sizeof(buffer), fp);
            if (!str)
                break;

            // specifically use fputs rather than puts, because fputs doesn't add
            // its own newline.
            fputs(str, stdout);
        }
    } else if (char_count) {
        char buffer[BUFSIZ];

        while (char_count) {
            int nread = fread(buffer, 1, min(BUFSIZ, char_count), fp);
            if (nread > 0) {
                int ncomplete = 0;

                while (ncomplete < nread) {
                    int nwrote = fwrite(&buffer[ncomplete], 1, nread - ncomplete, stdout);
                    if (nwrote > 0)
                        ncomplete += nwrote;

                    if (feof(stdout)) {
                        fprintf(stderr, "unexpected eof writing to stdout\n");
                        return 1;
                    }

                    if (ferror(stdout)) {
                        fprintf(stderr, "error writing to stdout\n");
                        return 1;
                    }
                }
            }

            char_count -= nread;

            if (feof(fp))
                break;

            if (ferror(fp)) {
                fprintf(stderr, "error reading input\n");
                break;
            }
        }
    }

    fclose(fp);

    if (print_filename) {
        puts("");
    }

    return 0;
}
