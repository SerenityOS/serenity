/*
 * Copyright (c) 2020, Matthew L. Curry <matthew.curry@gmail.com>
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

#include <AK/RefPtr.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

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

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath", nullptr) > 0) {
        perror("pledge");
        return 1;
    }

    const char* inpath = nullptr;
    const char* outpath = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(inpath, "Input file", "input", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(outpath, "Output file", "output", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

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
