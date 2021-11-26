/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/file.h>

extern "C" {

int flock(int fd, int operation)
{
    struct flock lock {
        short(operation & 0b11), SEEK_SET, 0, 0, 0
    };
    if (operation & LOCK_NB) {
        return fcntl(fd, F_SETLK, &lock);
    }

    // FIXME: Implement F_SETLKW in fcntl.
    VERIFY_NOT_REACHED();
}
}
