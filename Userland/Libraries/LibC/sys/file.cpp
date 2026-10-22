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
    short type = F_RDLCK;
    if (operation & LOCK_UN)
        type = F_UNLCK;
    else if (operation & LOCK_EX)
        type = F_WRLCK;
    struct flock lock {
        type, SEEK_SET, 0, 0, 0
    };
    return fcntl(fd, (operation & LOCK_NB) ? F_SETLK : F_SETLKW, &lock);
}
}
