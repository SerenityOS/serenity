/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <fnmatch.h>

int fnmatch(char const*, char const*, int)
{
    dbgln("FIXME: Implement fnmatch()");
    return 0;
}
