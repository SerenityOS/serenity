/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
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
    return fcntl(fd, (operation & LOCK_NB) ? F_SETLK : F_SETLKW, &lock);
}
}
