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

#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <syscall.h>

#define SC_NARG 4

FlatPtr arg[SC_NARG];
char buf[BUFSIZ];

static FlatPtr parse(const char* s);

int main(int argc, char** argv)
{
    bool output_buffer = false;
    bool list_syscalls = false;
    Vector<const char*> arguments;

    Core::ArgsParser args_parser;
    args_parser.add_option(output_buffer, "Output the contents of the buffer (beware of stray zero bytes!)", "output-buffer", 'o');
    args_parser.add_option(list_syscalls, "List all existing syscalls", "list-syscalls", 'l');
    args_parser.add_positional_argument(arguments, "Syscall arguments (can be strings, 'buf' for the output buffer, or numbers like 1234 or 0xffffffff)", "syscall-arguments");
    args_parser.parse(argc, argv);

    for (size_t i = 0; i < arguments.size(); i++) {
        arg[i] = parse(arguments[i]);
    }

    for (int sc = 0; sc < Syscall::Function::__Count; ++sc) {
        if (strcmp(Syscall::to_string((Syscall::Function)sc), (char*)arg[0]) == 0) {
            int rc = syscall(sc, arg[1], arg[2], arg[3]);
            if (rc == -1) {
                perror("syscall");
            } else {
                if (output_buffer)
                    fwrite(buf, 1, sizeof(buf), stdout);
            }

            fprintf(stderr, "Syscall return: %d\n", rc);
            return 0;
        }
    }

    fprintf(stderr, "Invalid syscall entry %s\n", (char*)arg[0]);
    return -1;
}

FlatPtr parse(const char* s)
{
    char* t;
    FlatPtr l;

    if (strcmp(s, "buf") == 0) {
        return (FlatPtr)buf;
    }

    l = strtoul(s, &t, 0);
    if (t > s && *t == 0) {
        return l;
    }

    return (FlatPtr)s;
}
