/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <pthread.h>
#include <stdio.h>
#include <sys/select.h>
#include <unistd.h>

int pipefds[2];

int main(int, char**)
{
    pipe(pipefds);

    pthread_t tid;
    pthread_create(
        &tid, nullptr, [](void*) -> void* {
            sleep(1);
            printf("ST: close()\n");
            close(pipefds[1]);
            pthread_exit(nullptr);
        },
        nullptr);

    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(pipefds[1], &rfds);

    printf("MT: select()\n");
    int rc = select(pipefds[1] + 1, &rfds, nullptr, nullptr, nullptr);
    if (rc < 0) {
        perror("select");
        return 1;
    }

    printf("ok\n");
    return 0;
}
