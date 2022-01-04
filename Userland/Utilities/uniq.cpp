/*
 * Copyright (c) 2020, Matthew L. Curry <matthew.curry@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

struct linebuf {
    char* buf = NULL;
    size_t len = 0;
};

static FILE* get_stream(const char* filepath, const char* perms)
{
    FILE* ret;

    if (filepath == nullptr) {
        if (perms[0] == 'r')
            return stdin;
        return stdout;
    }

    ret = fopen(filepath, perms);
    if (ret == nullptr) {
        perror("fopen");
        exit(1);
    }

    return ret;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    const char* inpath = nullptr;
    const char* outpath = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(inpath, "Input file", "input", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(outpath, "Output file", "output", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    FILE* infile = get_stream(inpath, "r");
    FILE* outfile = get_stream(outpath, "w");

    struct linebuf buffers[2];
    struct linebuf* previous = &(buffers[0]);
    struct linebuf* current = &(buffers[1]);
    bool first_run = true;
    for (;;) {
        errno = 0;
        ssize_t rc = getline(&(current->buf), &(current->len), infile);
        if (rc < 0 && errno != 0) {
            perror("getline");
            exit(1);
        }
        if (rc < 0)
            break;
        if (!first_run && strcmp(current->buf, previous->buf) == 0)
            continue;

        fputs(current->buf, outfile);
        swap(current, previous);
        first_run = false;
    }

    fclose(infile);
    fclose(outfile);

    return 0;
}
