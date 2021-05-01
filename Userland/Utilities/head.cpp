/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

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
    args_parser.set_general_help("Print the beginning ('head') of a file.");
    args_parser.add_option(line_count, "Number of lines to print (default 10)", "lines", 'n', "number");
    args_parser.add_option(char_count, "Number of characters to print", "characters", 'c', "number");
    args_parser.add_option(never_print_filenames, "Never print filenames", "quiet", 'q');
    args_parser.add_option(always_print_filenames, "Always print filenames", "verbose", 'v');
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
