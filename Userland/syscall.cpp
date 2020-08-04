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

#include <Kernel/API/Syscall.h>
#include <errno.h>
#include <getopt.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#if !defined __ENUMERATE_SYSCALL
#    define __ENUMERATE_SYSCALL(x) SC_##x,
#endif

#define SC_NARG 4

Syscall::Function syscall_table[] = {
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
};

FlatPtr arg[SC_NARG];
char buf[BUFSIZ];

FlatPtr parse(char* s);

int main(int argc, char** argv)
{
    int oflag;
    int opt;
    while ((opt = getopt(argc, argv, "olh")) != -1) {
        switch (opt) {
        case 'o':
            oflag = 1;
            break;
        case 'l':
            for (auto sc : syscall_table) {
                fprintf(stdout, "%s ", Syscall::to_string(sc));
            }
            return EXIT_SUCCESS;
        case 'h':
            fprintf(stderr, "usage: \tsyscall [-o] [-l] [-h] <syscall-name> <args...> [buf==BUFSIZ buffer]\n");
            fprintf(stderr, "\tsyscall write 1 hello 5\n");
            fprintf(stderr, "\tsyscall -o read 0 buf 5\n");
            fprintf(stderr, "\tsyscall sleep 3\n");
            break;
        default:
            exit(EXIT_FAILURE);
        }
    }

    if (optind >= argc) {
        fprintf(stderr, "No entry specified\n");
        return -1;
    }

    for (int i = 0; i < argc - optind; i++) {
        arg[i] = parse(argv[i + optind]);
    }

    for (auto sc : syscall_table) {
        if (strcmp(Syscall::to_string(sc), (char*)arg[0]) == 0) {
            int rc = syscall(sc, arg[1], arg[2], arg[3]);
            if (rc == -1) {
                perror("syscall");
            } else {
                if (oflag)
                    fwrite(buf, 1, sizeof(buf), stdout);
            }

            fprintf(stderr, "Syscall return: %d\n", rc);
            return 0;
        }
    }

    fprintf(stderr, "Invalid syscall entry %s\n", (char*)arg[0]);
    return -1;
}

FlatPtr parse(char* s)
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
