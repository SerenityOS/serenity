#include <Kernel/Syscall.h>
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
#if !defined __ENUMERATE_REMOVED_SYSCALL
#    define __ENUMERATE_REMOVED_SYSCALL(x)
#endif

#define SC_NARG 4

Syscall::Function syscall_table[] = {
    ENUMERATE_SYSCALLS
};

uintptr_t arg[SC_NARG];
char buf[BUFSIZ];

uintptr_t parse(char* s);

int main(int argc, char** argv)
{
    int oflag;
    int opt;
    while ((opt = getopt(argc, argv, "oh")) != -1) {
        switch (opt) {
        case 'o':
            oflag = 1;
            break;
        case 'h':
            fprintf(stderr, "usage: \tsyscall [-o] entry [args; buf==BUFSIZ buffer]\n");
            fprintf(stderr, "\tsyscall write 1 hello 5\n");
            fprintf(stderr, "\tsyscall -o read 0 buf 5\n");
            fprintf(stderr, "\tsyscall -o getcwd buf 100\n");
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
                    printf("%s", buf);
            }

            fprintf(stderr, "Syscall return: %d\n", rc);
            return 0;
        }
    }

    fprintf(stderr, "Invalid syscall entry %s\n", (char*)arg[0]);
    return -1;
}

uintptr_t parse(char* s)
{
    char* t;
    uintptr_t l;

    if (strcmp(s, "buf") == 0) {
        return (uintptr_t)buf;
    }

    l = strtoul(s, &t, 0);
    if (t > s && *t == 0) {
        return l;
    }

    return (uintptr_t)s;
}
