/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <assert.h>
#include <sys/resource.h>
#include <ulimit.h>

extern "C" {

long ulimit([[maybe_unused]] int cmd, [[maybe_unused]] long newlimit)
{
    dbgln("FIXME: Implement getrusage()");
    TODO();
    return -1;
}

int getrusage([[maybe_unused]] int who, [[maybe_unused]] struct rusage* usage)
{
    dbgln("FIXME: Implement getrusage()");
    return -1;
}

int getrlimit([[maybe_unused]] int resource, rlimit* rl)
{
    rl->rlim_cur = RLIM_INFINITY;
    rl->rlim_max = RLIM_INFINITY;
    return 0;
}

int setrlimit([[maybe_unused]] int resource, [[maybe_unused]] rlimit const* rl)
{
    return 0;
}
}
