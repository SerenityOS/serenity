/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <pthread.h>
#include <stdio.h>
#include <sys/select.h>
#include <unistd.h>

int main(int, char**)
{
    pthread_t tid;
    pthread_create(
        &tid, nullptr, [](void*) -> void* {
            sleep(1);
            __builtin_trap();
            return nullptr;
        },
        nullptr);

    pthread_join(tid, nullptr);

    printf("ok\n");
    return 0;
}
