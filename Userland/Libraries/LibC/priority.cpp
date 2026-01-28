/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <sys/resource.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpriority.html
int getpriority([[maybe_unused]] int which, [[maybe_unused]] id_t who)
{
    // FIXME: Implement getpriority()
    errno = ENOSYS;
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setpriority.html
int setpriority([[maybe_unused]] int which, [[maybe_unused]] id_t who, [[maybe_unused]] int value)
{
    // FIXME: Implement setpriority()
    errno = ENOSYS;
    return -1;
}
}
