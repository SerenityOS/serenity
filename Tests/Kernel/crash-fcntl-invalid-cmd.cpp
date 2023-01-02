/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    int rc = fcntl(0, -42);
    if (rc != -1) {
        printf("FAIL: rc was %d, instead of -1\n", rc);
        return 1;
    } else if (errno != EINVAL) {
        printf("FAIL: errno was %d, instead of EINVAL=%d\n", errno, EINVAL);
        return 1;
    } else {
        printf("PASS\n");
    }
    return 0;
}
