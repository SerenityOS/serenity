/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>

struct Options {
    String data;
    String type { "text" };
};

void print_usage(FILE* stream, const char* argv0)
{
    fprintf(
        stream,
        "Usage:\n"
        "\t%s [--type type] text\n"
        "\t%s [--type type] < file\n"
        "\n"
        "\t-t type, --type type\tPick a type.\n"
        "\t-h, --help\t\tPrint this help message.\n",
        argv0,
        argv0);
}

Options parse_options(int argc, char* argv[])
{
    Options options;

    static struct option long_options[] = {
        { "type", required_argument, 0, 't' },
        { "help", no_argument, 0, 'h' },
        { 0, 0, 0, 0 }
    };
    while (true) {
        int option_index;
        int c = getopt_long(argc, argv, "t:h", long_options, &option_index);
        if (c == -1)
            break;
        if (c == 0)
            c = long_options[option_index].val;

        switch (c) {
        case 't':
            options.type = optarg;
            break;
        case 'h':
            print_usage(stdout, argv[0]);
            exit(0);
        default:
            print_usage(stderr, argv[0]);
            exit(1);
        }
    }

    if (optind < argc) {
        // Copy the rest of our command-line args.
        StringBuilder builder;
        bool first = true;
        for (int i = optind; i < argc; i++) {
            if (!first)
                builder.append(' ');
            first = false;
            builder.append(argv[i]);
        }
        options.data = builder.to_string();
    } else {
        // Copy our stdin.
        auto c_stdin = CFile::construct();
        bool success = c_stdin->open(
            STDIN_FILENO,
            CIODevice::OpenMode::ReadOnly,
            CFile::ShouldCloseFileDescription::No);
        ASSERT(success);
        auto buffer = c_stdin->read_all();
        dbg() << "Read size " << buffer.size();
        options.data = String((char*)buffer.data(), buffer.size());
    }

    return options;
}

int main(int argc, char* argv[])
{
    GApplication app(argc, argv);

    Options options = parse_options(argc, argv);

    GClipboard& clipboard = GClipboard::the();
    clipboard.set_data(options.data, options.type);

    return 0;
}
