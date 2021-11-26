/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <sys/resource.h>

extern "C" {

int getpriority([[maybe_unused]] int which, [[maybe_unused]] id_t who)
{
    dbgln("FIXME: Implement getpriority()");
    return -1;
}

int setpriority([[maybe_unused]] int which, [[maybe_unused]] id_t who, [[maybe_unused]] int value)
{
    dbgln("FIXME: Implement setpriority()");
    return -1;
}
}
