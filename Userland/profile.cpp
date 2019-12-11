#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        printf("usage: profile <pid> <on|off>\n");
        return 0;
    }

    pid_t pid = atoi(argv[1]);
    bool enabled = !strcmp(argv[2], "on");

    if (enabled) {
        if (profiling_enable(pid) < 0) {
            perror("profiling_enable");
            return 1;
        }
        return 0;
    }

    if (profiling_disable(pid) < 0) {
        perror("profiling_disable");
        return 1;
    }
    return 0;
}
