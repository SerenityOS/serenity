/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <assert.h>
#include <sys/resource.h>
#include <syscall.h>
#include <ulimit.h>

extern "C" {

long ulimit([[maybe_unused]] int cmd, [[maybe_unused]] long newlimit)
{
    dbgln("FIXME: Implement ulimit()");
    TODO();
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/009696699/functions/getrusage.html
int getrusage(int who, struct rusage* usage)
{
    int rc = syscall(SC_getrusage, who, usage);
    __RETURN_WITH_ERRNO(rc, rc, -1);
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
