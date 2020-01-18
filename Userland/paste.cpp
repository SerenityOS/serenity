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

#include <AK/String.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>

struct Options {
    bool print_type { false };
    bool no_newline { false };
};

void print_usage(FILE* stream, const char* argv0)
{
    fprintf(
        stream,
        "Usage:\n"
        "\t%s [--print-type] [--no-newline]\n"
        "\n"
        "\t--print-type\t\tDisplay the copied type.\n"
        "\t-n, --no-newline\tDo not append a newline.\n"
        "\t-h, --help\t\tPrint this help message.\n",
        argv0);
}

Options parse_options(int argc, char* argv[])
{
    Options options;

    static struct option long_options[] = {
        { "print-type", no_argument, 0, 'p' },
        { "no-newline", no_argument, 0, 'n' },
        { "help", no_argument, 0, 'h' },
        { 0, 0, 0, 0 }
    };
    while (true) {
        int option_index;
        int c = getopt_long(argc, argv, "hn", long_options, &option_index);
        if (c == -1)
            break;
        if (c == 0)
            c = long_options[option_index].val;

        switch (c) {
        case 'p':
            options.print_type = true;
            break;
        case 'n':
            options.no_newline = true;
            break;
        case 'h':
            print_usage(stdout, argv[0]);
            exit(0);
        default:
            print_usage(stderr, argv[0]);
            exit(1);
        }
    }

    return options;
}

int main(int argc, char* argv[])
{
    GApplication app(argc, argv);

    Options options = parse_options(argc, argv);

    GClipboard& clipboard = GClipboard::the();
    auto data_and_type = clipboard.data_and_type();

    if (!options.print_type) {
        printf("%s", data_and_type.data.characters());
        // Append a newline to text contents, but
        // only if we're not asked not to do this.
        if (data_and_type.type == "text" && !options.no_newline)
            putchar('\n');
    } else {
        printf("%s\n", data_and_type.type.characters());
    }

    return 0;
}
