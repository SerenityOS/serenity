/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <sys/auxv.h>
#include <sys/internals.h>

extern "C" {

long getauxval(long type)
{
    errno = 0;

    auxv_t* auxvp = (auxv_t*)__auxiliary_vector;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == type)
            return auxvp->a_un.a_val;
    }
    errno = ENOENT;
    return 0;
}
}
