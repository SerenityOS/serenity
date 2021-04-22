/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc < 3) {
        printf("usage: flock <path> <command...>\n");
        return 0;
    }

    pid_t child_pid;
    if ((errno = posix_spawnp(&child_pid, argv[2], nullptr, nullptr, &argv[2], environ))) {
        perror("posix_spawn");
        return 1;
    }

    int status;
    if (waitpid(child_pid, &status, 0) < 0) {
        perror("waitpid");
        return 1;
    }
    return WEXITSTATUS(status);
}
