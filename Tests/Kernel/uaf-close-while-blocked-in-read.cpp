/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int pipefds[2];

int main(int, char**)
{
    pipe(pipefds);

    pthread_t tid;
    pthread_create(
        &tid, nullptr, [](void*) -> void* {
            sleep(1);
            printf("Second thread closing pipes!\n");
            close(pipefds[0]);
            close(pipefds[1]);
            pthread_exit(nullptr);
        },
        nullptr);

    printf("First thread doing a blocking read from pipe...\n");
    char buffer[16];
    ssize_t nread = read(pipefds[0], buffer, sizeof(buffer));
    if (nread != 0) {
        printf("FAIL, read %zd bytes from pipe\n", nread);
        return 1;
    }

    printf("PASS\n");

    return 0;
}
