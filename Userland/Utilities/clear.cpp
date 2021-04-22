/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    printf("\033[3J\033[H\033[2J");
    fflush(stdout);
    return 0;
}
