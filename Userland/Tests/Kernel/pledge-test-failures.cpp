/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>
#include <unistd.h>

int main()
{
    int res = pledge("stdio unix rpath", "stdio");
    if (res < 0) {
        perror("pledge");
        return 1;
    }

    res = pledge("stdio unix", "stdio unix");
    if (res >= 0) {
        fprintf(stderr, "second pledge should have failed\n");
        return 1;
    }

    res = pledge("stdio rpath", "stdio");
    if (res < 0) {
        perror("pledge");
        return 1;
    }

    printf("PASS\n");
    return 0;
}
